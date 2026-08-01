package com.hogimn.malcollector.anime

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.hogimn.malcollector.circuitbreakersupport.CircuitBreaker
import com.hogimn.malcollector.discoverysupport.DiscoveryClient
import com.hogimn.malcollector.restsupport.RestTemplate

open class PollClient(val mapper: ObjectMapper, val template: RestTemplate) {
    private val circuitBreaker = CircuitBreaker()

    open fun fetchPollContentIds(contentType: String): Set<Int> {
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

    open fun fetchEpisodeDistribution(contentId: Int, contentType: String): Map<Int, Map<String, Any>>? {
        return try {
            val endpoint = DiscoveryClient(mapper, template).getUrl("poll")

            val response = circuitBreaker.withCircuitBreaker({
                template.get("$endpoint/poll/summary?contentId=$contentId&contentType=$contentType", "application/json")
            }, fallback())

            if (response.isNullOrBlank()) {
                return null
            }

            val jsonNode: JsonNode = mapper.readTree(response)
            val distributionNode = jsonNode.get("episodeDistribution") ?: return null

            mapper.convertValue(distributionNode, object : TypeReference<Map<Int, Map<String, Any>>>() {})
        } catch (_: Exception) {
            null
        }
    }

    private fun fallback(): () -> Nothing? = { null }
}