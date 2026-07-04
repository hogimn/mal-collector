package com.hogimn.malchart.discovery

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.hogimn.malchart.restsupport.RestTemplate
import org.slf4j.LoggerFactory
import java.util.*

open class DiscoveryClient(val mapper: ObjectMapper, val template: RestTemplate) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private fun <E> List<E>.random(random: Random): E? = if (isNotEmpty()) get(random.nextInt(size)) else null

    fun getUrl(appId: String): String? {
        val endpoint = System.getenv("DISCOVERY_SERVER_ENDPOINT")
        val response = template.get("$endpoint/discovery/apps", "application/json", Pair("appId", appId))
        val instances: List<InstanceInfo> = mapper.readValue(response, object : TypeReference<List<InstanceInfo>>() {})

        logger.info("Found ${instances.size} instance(s) of $appId.")

        return when {
            instances.isEmpty() -> null
            else -> instances.random(Random())!!.url
        }
    }

    fun heartbeat(appId: String, url: String) {
        val endpoint = System.getenv("DISCOVERY_SERVER_ENDPOINT")
        try {
            val newUrl = if(url.last() == '/') url.dropLast(1) else url
            val data = "{\"appId\":\"$appId\",\"url\":\"$newUrl\"}"
            template.post("$endpoint/discovery/apps", "application/json", data)
        } catch (e: Exception) {
            logger.error("Unable to contact discovery server. $endpoint", e)
        }
    }

    private data class InstanceInfo(val appId: String, val url: String)
}
