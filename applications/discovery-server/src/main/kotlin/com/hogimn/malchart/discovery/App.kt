package com.hogimn.malchart.discovery

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.hogimn.malchart.redissupport.RedisConfig
import com.hogimn.malchart.restsupport.BasicServer
import com.hogimn.malchart.restsupport.DefaultController
import java.lang.System.getenv
import java.util.*

class App(val host: String, val password: String, port: Int) : BasicServer(port) {
    val mapper: ObjectMapper = ObjectMapper()
        .registerKotlinModule()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    override fun registerContexts() {
        val client = RedisConfig().getClient(host, password)

        context("/discovery/apps", DiscoveryController(mapper, InstanceDataGateway(client, 60000L)))
        context("/", DefaultController())
    }
}

fun main() {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    val host = getenv("REDIS_HOST")
    val password = getenv("REDIS_PASSWORD")
    val port = getenv("PORT").toInt()
    App(host, password, port).start()
}
