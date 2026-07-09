package com.hogimn.malcollector.gateway

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.hogimn.malcollector.discoverysupport.DiscoveryClient
import com.hogimn.malcollector.restsupport.BasicServer
import com.hogimn.malcollector.restsupport.DefaultController
import com.hogimn.malcollector.restsupport.RestTemplate
import java.lang.System.getenv
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class App(port: Int) : BasicServer(port) {
    val mapper: ObjectMapper = ObjectMapper()
        .registerKotlinModule()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    val restTemplate = RestTemplate()
    val discoveryClient = DiscoveryClient(mapper, restTemplate)

    override fun registerContexts() {
        context("/api", GatewayController(restTemplate, discoveryClient))
        context("/", DefaultController())
    }

    override fun start() {
        super.start()
        Executors.newSingleThreadScheduledExecutor(Executors.defaultThreadFactory()).scheduleAtFixedRate({
            discoveryClient.heartbeat("gateway", uri())
        }, 0L, 30L, TimeUnit.SECONDS)
    }
}

fun main() {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    val port = getenv("PORT")?.toInt() ?: 8000
    App(port).start()
}