package com.hogimn.malcollector.restsupport

import com.sun.net.httpserver.HttpExchange
import java.net.URLDecoder
import java.sql.SQLException

abstract class BasicController {
    private val logger = AppLoggerFactory.getLogger(javaClass)

    abstract fun handle(exchange: HttpExchange): Boolean

    protected fun get(
        exchange: HttpExchange,
        uri: String,
        supportedMediaTypes: List<String>,
        condition: (Map<String, String>) -> Boolean = { true },
        block: () -> String
    ): Boolean {
        if (!matchRoute(exchange, "GET", uri, condition)) return false
        return respond(exchange, supportedMediaTypes, 200, block)
    }

    protected fun post(
        exchange: HttpExchange,
        uri: String,
        supportedMediaTypes: List<String>,
        condition: (Map<String, String>) -> Boolean = { true },
        block: () -> String
    ): Boolean {
        if (!matchRoute(exchange, "POST", uri, condition)) return false
        return respond(exchange, supportedMediaTypes, 201, block)
    }

    protected fun put(
        exchange: HttpExchange,
        uri: String,
        supportedMediaTypes: List<String>,
        condition: (Map<String, String>) -> Boolean = { true },
        block: () -> String
    ): Boolean {
        if (!matchRoute(exchange, "PUT", uri, condition)) return false
        return respond(exchange, supportedMediaTypes, 200, block)
    }

    private fun matchRoute(
        exchange: HttpExchange,
        method: String,
        uri: String,
        condition: (Map<String, String>) -> Boolean
    ): Boolean {
        if (exchange.requestMethod != method) return false
        if (exchange.requestURI.path != uri) return false
        if (!condition(parameters(exchange))) return false
        return true
    }

    private fun respond(
        exchange: HttpExchange,
        supportedMediaTypes: List<String>,
        successStatus: Int,
        block: () -> String
    ): Boolean {
        val acceptedMediaType = exchange.requestHeaders.getFirst("Accept")

        if (acceptedMediaType !in supportedMediaTypes) return false

        try {
            exchange.responseHeaders.add("Content-Type", acceptedMediaType)
            sendResponse(exchange, successStatus, block())
        } catch (e: IllegalStateException) {
            sendResponse(exchange, 422, e.message.toString())
        } catch (e: SQLException) {
            sendResponse(exchange, 500, e.message.toString())
        } catch (e: Exception) {
            logger.error(e.message, e)
            sendResponse(exchange, 500, e.message.toString())
        }

        return true
    }

    protected fun parameters(exchange: HttpExchange): Map<String, String> {
        val query = exchange.requestURI.query ?: return emptyMap()

        return query.split("&").filter { it.contains("=") }.associate { pair ->
            val (key, value) = pair.split("=", limit = 2)
            URLDecoder.decode(key, "UTF-8") to URLDecoder.decode(value, "UTF-8")
        }
    }

    protected fun body(exchange: HttpExchange): String = exchange.getAttribute("body") as String

    protected fun sendResponse(exchange: HttpExchange, status: Int, body: String) {
        if (body.isEmpty()) {
            logger.info("[Z] Sending response - Status: $status (No Body)")
        } else {
            logger.info("[Z] Sending response - Status: $status, Body: $body")
        }

        val bytes = body.toByteArray()
        exchange.sendResponseHeaders(status, bytes.size.toLong())
        exchange.responseBody.write(bytes)
        exchange.close()
    }
}