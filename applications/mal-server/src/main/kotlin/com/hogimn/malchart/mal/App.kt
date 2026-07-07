package com.hogimn.malchart.mal

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
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

class App(port: Int, val malClientId: String) : BasicServer(port) {
    val mapper: ObjectMapper = ObjectMapper()
        .registerKotlinModule()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    override fun registerContexts() {
        val animeClient = AnimeClient(mapper, RestTemplate(), MalProvider(malClientId))
        val animePollClient = AnimePollClient(mapper, RestTemplate(), MalProvider(malClientId))
        context("/mal", MalController(animeClient, animePollClient))
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
    val malClientId = getenv("MAL_CLIENT_ID")
        ?: throw IllegalStateException("Environment variable 'MAL_CLIENT_ID' is not set.")
    App(port, malClientId).start()
}
