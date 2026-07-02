package com.hogimn.malchart.restsupport

import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

class RestTemplate {
    private val client = HttpClient.newHttpClient()

    fun get(endpoint: String, accept: String, vararg pairs: Pair<String, String>): String {
        val query = pairs.joinToString("&") { (name, value) ->
            "${encode(name)}=${encode(value)}"
        }
        val uri = if (query.isEmpty()) URI(endpoint) else URI("$endpoint?$query")
        val request = HttpRequest.newBuilder(uri)
            .header("Accept", accept)
            .GET()
            .build()
        return execute(request)
    }

    fun post(endpoint: String, accept: String, data: String): String {
        val request = HttpRequest.newBuilder(URI(endpoint))
            .header("Accept", accept)
            .header("Content-type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(data))
            .build()
        return execute(request)
    }

    fun put(endpoint: String, accept: String, data: String): String {
        val request = HttpRequest.newBuilder(URI(endpoint))
            .header("Accept", accept)
            .header("Content-Type", "application/json")
            .PUT(HttpRequest.BodyPublishers.ofString(data))
            .build()
        return execute(request)
    }

    private fun execute(request: HttpRequest): String {
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() >= 300) {
            return "status_code ${response.statusCode()}"
        }
        return response.body()
    }

    private fun encode(value: String) = URLEncoder.encode(value, StandardCharsets.UTF_8)
}
