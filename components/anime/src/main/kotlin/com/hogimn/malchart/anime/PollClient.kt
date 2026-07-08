package com.hogimn.malchart.anime

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.hogimn.malchart.circuitbreaker.CircuitBreaker
import com.hogimn.malchart.discovery.DiscoveryClient
import com.hogimn.malchart.restsupport.RestTemplate

class PollClient(val mapper: ObjectMapper, val template: RestTemplate) {
    private val circuitBreaker = CircuitBreaker()

    fun fetchPollContentIds(contentType: String): Set<Int> {
        val endpoint = DiscoveryClient(mapper, template).getUrl("poll")

        val response = circuitBreaker.withCircuitBreaker({
            template.get("$endpoint/poll/content-ids?contentType=$contentType", "application/json")
        }, fallback())

        if (response.isNullOrBlank()) {
            return emptySet()
        }

        val idList: List<Int> = mapper.readValue(response, object : TypeReference<List<Int>>() {})
        return idList.toSet()
    }

    private fun fallback(): () -> Nothing? = { null }
}