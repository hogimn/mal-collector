package com.hogimn.malchart.restsupport

import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

class RestTemplate {
    private val client = HttpClient.newHttpClient()
    private val logger = AppLoggerFactory.getLogger(javaClass)

    fun get(endpoint: String, accept: String, vararg pairs: Pair<String, String>): String {
        val uri = buildUri(endpoint, pairs)
        val request = HttpRequest.newBuilder(uri)
            .header("Accept", accept)
            .GET()
            .build()
        return execute(request)
    }

    fun post(endpoint: String, accept: String, data: String): String {
        return post(endpoint, accept, data, *arrayOf())
    }

    fun post(endpoint: String, accept: String, data: String, vararg pairs: Pair<String, String>): String {
        val uri = buildUri(endpoint, pairs)
        val request = HttpRequest.newBuilder(uri)
            .header("Accept", accept)
            .header("Content-Type", "application/json") // 대문자 T 통일
            .POST(HttpRequest.BodyPublishers.ofString(data))
            .build()
        return execute(request, data)
    }

    fun put(endpoint: String, accept: String, data: String): String {
        return put(endpoint, accept, data, *arrayOf())
    }

    fun put(endpoint: String, accept: String, data: String, vararg pairs: Pair<String, String>): String {
        val uri = buildUri(endpoint, pairs)
        val request = HttpRequest.newBuilder(uri)
            .header("Accept", accept)
            .header("Content-Type", "application/json")
            .PUT(HttpRequest.BodyPublishers.ofString(data))
            .build()
        return execute(request, data)
    }

    private fun buildUri(endpoint: String, pairs: Array<out Pair<String, String>>): URI {
        if (pairs.isEmpty()) return URI(endpoint)

        val query = pairs.joinToString("&") { (name, value) ->
            "${encode(name)}=${encode(value)}"
        }

        return URI("$endpoint?$query")
    }

    private fun execute(request: HttpRequest, body: String? = null): String {
        if (body.isNullOrEmpty()) {
            logger.info("[S] Sending Request -> [${request.method()}] ${request.uri()} (No Body)")
        } else {
            logger.info("[S] Sending Request -> [${request.method()}] ${request.uri()}, Body: $body")
        }

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())

        logger.info("[R] Received Response - Status: ${response.statusCode()}, Body: ${response.body()}")

        if (response.statusCode() >= 300) {
            return "status_code ${response.statusCode()}"
        }
        return response.body()
    }

    private fun encode(value: String) = URLEncoder.encode(value, StandardCharsets.UTF_8)
}