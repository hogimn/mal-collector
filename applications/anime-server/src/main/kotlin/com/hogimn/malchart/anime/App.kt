package com.hogimn.malchart.anime

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.hogimn.malchart.discovery.DiscoveryClient
import com.hogimn.malchart.jdbcsupport.DataSourceConfig
import com.hogimn.malchart.jdbcsupport.JdbcTemplate
import com.hogimn.malchart.restsupport.BasicServer
import com.hogimn.malchart.restsupport.DefaultController
import com.hogimn.malchart.restsupport.RestTemplate
import java.lang.System.getenv
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class App(val url: String, port: Int) : BasicServer(port) {
    val mapper: ObjectMapper = ObjectMapper()
        .registerKotlinModule()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    override fun registerContexts() {
        val dataSource = DataSourceConfig().createDataSource(url)
        val template = JdbcTemplate(dataSource)

        context("/anime", AnimeController(mapper, AnimeDataGateway(template), PollClient(mapper, RestTemplate())))
        context("/", DefaultController())
    }

    override fun start() {
        super.start()
        Executors.newSingleThreadScheduledExecutor(Executors.defaultThreadFactory()).scheduleAtFixedRate({
            DiscoveryClient(mapper, RestTemplate()).heartbeat("anime", uri())
        }, 0L, 30L, TimeUnit.SECONDS)
    }
}

fun main() {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    val url = getenv("DATABASE_URL")
    val port = getenv("PORT").toInt()
    App(url, port).start()
}
