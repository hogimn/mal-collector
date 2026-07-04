package com.hogimn.malchart.restsupport

import com.sun.net.httpserver.HttpServer
import com.sun.net.httpserver.HttpsServer
import org.slf4j.LoggerFactory
import java.net.InetAddress
import java.net.InetSocketAddress

abstract class BasicServer(port: Int) {
    protected val server: HttpServer = HttpServer.create(InetSocketAddress(port), 0)
    private val logger = LoggerFactory.getLogger(javaClass)

    abstract fun registerContexts()

    open fun start() {
        registerContexts()
        server.start()
    }

    fun stop() {
        server.stop(0)
    }

    fun uri(): String {
        val scheme = if (server is HttpsServer) "https" else "http"
        return "$scheme://${InetAddress.getLocalHost().hostAddress}:${server.address.port}"
    }

    protected fun context(path: String, vararg controllers: BasicController) {
        server.createContext(path) { exchange ->
            val method = exchange.requestMethod
            val uri = exchange.requestURI

            val body = exchange.requestBody.bufferedReader().readText()
            exchange.setAttribute("body", body)

            if (body.isEmpty()) {
                logger.info("Received Request -> [$method] $uri (No Body)")
            } else {
                logger.info("Received Request -> [$method] $uri, Body: $body")
            }

            val handled = controllers.any { it.handle(exchange) }
            if (!handled) {
                exchange.sendResponseHeaders(404, -1)
                exchange.close()
            }
        }
    }
}
