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

    open fun findByYearAndSeason(year: Int, season: String): List<AnimeInfo> {
        val endpoint = discoveryClient.getUrl(SERVICE_NAME)

        return circuitBreaker.withCircuitBreaker({
            val response = template.get("$endpoint/anime?year=$year&season=$season", "application/json")
            mapper.readValue(
                response,
                mapper.typeFactory.constructCollectionType(List::class.java, AnimeInfo::class.java)
            ) as List<AnimeInfo>
        }, { emptyList() })
    }
}