package test.hogimn.malchart.gateway

import com.fasterxml.jackson.core.type.TypeReference
import com.hogimn.malchart.discovery.DiscoveryClient
import com.hogimn.malchart.gateway.GatewayController
import com.hogimn.malchart.restsupport.BasicController
import com.hogimn.malchart.restsupport.BasicServer
import com.hogimn.malchart.testsupport.TestControllerSupport
import com.sun.net.httpserver.HttpExchange
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.concurrent.Executors
import kotlin.test.assertEquals

class GatewayControllerTest : TestControllerSupport() {
    private val stubDiscoveryClient = object : DiscoveryClient(mapper, template) {
        override fun getUrl(appId: String): String {
            return "http://localhost:8086"
        }
    }

    private val server = object : BasicServer(8086) {
        override fun registerContexts() {
            server.executor = Executors.newCachedThreadPool()
            context("/api", GatewayController(template, stubDiscoveryClient))

            context("/anime", object : BasicController() {
                override fun handle(exchange: HttpExchange): Boolean {
                    val path = exchange.requestURI.path
                    val method = exchange.requestMethod
                    val accept = exchange.requestHeaders.getFirst("Accept") ?: "application/json"

                    exchange.responseHeaders.add("Content-Type", accept)

                    when (method) {
                        "GET" if path == "/anime/by-year-and-season" -> {
                            val year = exchange.requestURI.query?.contains("year=2026") ?: false
                            val responseBody = if (year) """{"status":"fetched_2026"}""" else """{"status":"unknown"}"""
                            sendResponse(exchange, responseBody)
                            return true
                        }

                        "POST" if path == "/anime/upsert" -> {
                            val requestBody = exchange.getAttribute("body") as String
                            sendResponse(exchange, """{"echo":$requestBody,"result":"success"}""")
                            return true
                        }

                        "POST" if path == "/anime/poll/by-year-and-season" -> {
                            val query = exchange.requestURI.query ?: ""
                            val hasYear = query.contains("year=2026")
                            val hasSeason = query.contains("season=summer")

                            val responseBody = """
                                                {
                                                    "success": true, 
                                                    "receivedQuery": "$query", 
                                                    "isValid": ${hasYear && hasSeason}
                                                }
                                            """.trimIndent()

                            sendResponse(exchange, responseBody)
                            return true
                        }

                        "PUT" if path == "/anime" -> {
                            val requestBody = exchange.getAttribute("body") as String
                            sendResponse(exchange, """{"updated":true,"data":$requestBody}""")
                            return true
                        }
                    }
                    return false
                }

                private fun sendResponse(exchange: HttpExchange, body: String) {
                    val bytes = body.toByteArray()
                    exchange.sendResponseHeaders(200, bytes.size.toLong())
                    exchange.responseBody.write(bytes)
                    exchange.close()
                }
            })
        }
    }

    @Before
    fun setUp() {
        server.start()
    }

    @After
    fun tearDown() {
        server.stop()
    }

    @Test
    fun testGatewayGetRouting() {
        val targetUrl = "http://localhost:8086/api/anime/by-year-and-season"
        val yearParam = Pair("year", "2026")

        val response = template.get(targetUrl, "application/json", yearParam)

        val actual: Map<String, String> = mapper.readValue(response, object : TypeReference<Map<String, String>>() {})
        assertEquals("fetched_2026", actual["status"])
    }

    @Test
    fun testGatewayPostRouting() {
        val targetUrl = "http://localhost:8086/api/anime/upsert"
        val animeData = mapOf("id" to 7777, "title" to "Chainsaw Man")
        val requestBody = mapper.writeValueAsString(animeData)

        val response = template.post(targetUrl, "application/json", requestBody)

        val actual: Map<String, Any> = mapper.readValue(response, object : TypeReference<Map<String, Any>>() {})
        assertEquals("success", actual["result"])
    }

    @Test
    fun testGatewayPostRoutingWithQueryParameters() {
        val targetUrl = "http://localhost:8086/api/anime/poll/by-year-and-season"

        val response = template.post(
            targetUrl,
            "application/json",
            "",
            Pair("year", "2026"),
            Pair("season", "summer")
        )

        val actual: Map<String, Any> = mapper.readValue(response, object : TypeReference<Map<String, Any>>() {})
        assertEquals(true, actual["success"])
    }

    @Test
    fun testGatewayPutRouting() {
        val targetUrl = "http://localhost:8086/api/anime"
        val updateData = mapOf("id" to 7777, "score" to 9.8)
        val requestBody = mapper.writeValueAsString(updateData)

        val response = template.put(targetUrl, "application/json", requestBody)

        val actual: Map<String, Any> = mapper.readValue(response, object : TypeReference<Map<String, Any>>() {})
        assertEquals(true, actual["updated"])
    }
}