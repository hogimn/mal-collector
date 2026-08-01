package com.hogimn.malcollector.poll

import com.fasterxml.jackson.databind.ObjectMapper
import com.hogimn.malcollector.circuitbreakersupport.CircuitBreaker
import com.hogimn.malcollector.discoverysupport.DiscoveryClient
import com.hogimn.malcollector.restsupport.RestTemplate

open class AnimeClient(
    val mapper: ObjectMapper,
    val template: RestTemplate,
    val discoveryClient: DiscoveryClient
) {
    private val circuitBreaker = CircuitBreaker()

    companion object {
        private const val SERVICE_NAME = "anime"
    }

    open fun findIdsByYearAndSeason(year: Int, season: String): List<Int> {
        val endpoint = discoveryClient.getUrl(SERVICE_NAME)

        return circuitBreaker.withCircuitBreaker({
            val response = template.get("$endpoint/anime-ids?year=$year&season=$season", "application/json")
            mapper.readValue(
                response,
                mapper.typeFactory.constructCollectionType(List::class.java, Int::class.javaObjectType)
            ) as List<Int>
        }, { emptyList() })
    }
}