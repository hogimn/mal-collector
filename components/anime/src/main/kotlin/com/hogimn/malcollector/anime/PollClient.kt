package com.hogimn.malcollector.anime

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.hogimn.malcollector.circuitbreakersupport.CircuitBreaker
import com.hogimn.malcollector.discoverysupport.DiscoveryClient
import com.hogimn.malcollector.restsupport.RestTemplate

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