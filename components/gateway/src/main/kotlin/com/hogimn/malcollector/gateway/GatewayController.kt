package com.hogimn.malcollector.gateway

import com.hogimn.malcollector.discoverysupport.DiscoveryClient
import com.hogimn.malcollector.restsupport.BasicController
import com.hogimn.malcollector.restsupport.RestTemplate
import com.sun.net.httpserver.HttpExchange

class GatewayController(
    private val template: RestTemplate,
    private val discoveryClient: DiscoveryClient
) : BasicController() {

    private val logger = AppLoggerFactory.getLogger(javaClass)

    override fun handle(exchange: HttpExchange): Boolean {
        val path = exchange.requestURI.path

        if (!path.startsWith("/api")) return false

        handleCors(exchange)
        if (exchange.requestMethod == "OPTIONS") {
            exchange.sendResponseHeaders(204, -1)
            exchange.close()
            return true
        }

        val segments = path.split("/").filter { it.isNotEmpty() }
        if (segments.size < 2) return false

        val serviceName = segments[1]
        val remainingPath = "/" + segments.drop(1).joinToString("/")

        val acceptHeader = exchange.requestHeaders.getFirst("Accept") ?: "application/json"

        val endpoint = discoveryClient.getUrl(serviceName)

        try {
            val queryString = exchange.requestURI.query?.let { "?$it" } ?: ""
            val fullTargetUrl = "$endpoint$remainingPath$queryString"

            val responseString = when (exchange.requestMethod) {
                "GET" -> template.get(fullTargetUrl, acceptHeader)
                "POST" -> template.post(fullTargetUrl, acceptHeader, body(exchange))
                "PUT" -> template.put(fullTargetUrl, acceptHeader, body(exchange))
                else -> return false
            }

            sendResponse(exchange, 200, responseString)
            return true

        } catch (e: Exception) {
            logger.error("Gateway Routing Error: ${e.message}", e)
            return false
        }
    }

    private fun handleCors(exchange: HttpExchange) {
        exchange.responseHeaders.add("Access-Control-Allow-Origin", "http://localhost:5173")
        exchange.responseHeaders.add("Access-Control-Allow-Methods", "GET, POST, PUT, OPTIONS")
        exchange.responseHeaders.add("Access-Control-Allow-Headers", "*")
        exchange.responseHeaders.add("Access-Control-Allow-Credentials", "true")
    }
}