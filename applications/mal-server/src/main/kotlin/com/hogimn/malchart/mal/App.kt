package com.hogimn.malchart.mal

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.hogimn.malchart.discovery.DiscoveryClient
import com.hogimn.malchart.malsupport.MalProvider
import com.hogimn.malchart.restsupport.BasicServer
import com.hogimn.malchart.restsupport.DefaultController
import com.hogimn.malchart.restsupport.RestTemplate
import java.lang.System.getenv
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class App(port: Int) : BasicServer(port) {
    val mapper: ObjectMapper = ObjectMapper().registerKotlinModule().registerModule(JavaTimeModule())
    val clientId: String = getenv("MAL_CLIENT_ID")

    override fun registerContexts() {
        val animeCollectClient = AnimeCollectClient(mapper, RestTemplate(), MalProvider(clientId))
        context("/mal", MalController(animeCollectClient))
        context("/", DefaultController())
    }

    override fun start() {
        super.start()
        Executors.newSingleThreadScheduledExecutor(Executors.defaultThreadFactory()).scheduleAtFixedRate({
            DiscoveryClient(mapper, RestTemplate()).heartbeat("mal", uri())
        }, 0L, 30L, TimeUnit.SECONDS)
    }
}

fun main() {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    val port = getenv("PORT").toInt()
    App(port).start()
}
