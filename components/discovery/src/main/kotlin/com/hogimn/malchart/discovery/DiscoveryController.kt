package com.hogimn.malchart.discovery

import com.fasterxml.jackson.databind.ObjectMapper
import com.sun.net.httpserver.HttpExchange
import com.hogimn.malchart.restsupport.BasicController

class DiscoveryController(val mapper: ObjectMapper, val gateway: InstanceDataGateway) : BasicController() {
    private val logger = AppLoggerFactory.getLogger(javaClass)

    override fun handle(exchange: HttpExchange): Boolean {
        return post(exchange, "/discovery/apps", listOf("application/json")) {
            val instance = mapper.readValue(body(exchange), InstanceInfo::class.java)
            val record = gateway.heartbeat(instance.appId, instance.url)
            logger.info("Registered application ${instance.appId}, ${instance.url}")
            mapper.writeValueAsString(InstanceInfo(record.appId, record.url))
        } || get(exchange, "/discovery/apps", listOf("application/json")) {
            val appId = parameters(exchange)["appId"]!!
            val list = gateway.findBy(appId).map { record ->
                InstanceInfo(record.appId, record.url)
            }
            mapper.writeValueAsString(list)
        }
    }
}