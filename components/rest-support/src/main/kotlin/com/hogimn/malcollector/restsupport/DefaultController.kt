package com.hogimn.malcollector.restsupport

import com.sun.net.httpserver.HttpExchange

class DefaultController : BasicController() {
    override fun handle(exchange: HttpExchange): Boolean {
        val bytes = "Noop!".toByteArray()
        exchange.responseHeaders.add("Content-Type", "text/html; charset=UTF-8")
        exchange.sendResponseHeaders(200, bytes.size.toLong())
        exchange.responseBody.write(bytes)
        exchange.close()
        return true
    }
}