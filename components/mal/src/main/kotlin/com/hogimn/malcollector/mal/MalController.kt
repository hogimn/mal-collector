package com.hogimn.malcollector.mal

import com.hogimn.malcollector.restsupport.BasicController
import com.sun.net.httpserver.HttpExchange

class MalController(
    val animeClient: AnimeClient, val pollClient: AnimePollClient
) : BasicController() {
    override fun handle(exchange: HttpExchange): Boolean {
        val mediaTypes = listOf("application/json", "application/vnd.malcollector.v1+json")

        return post(
            exchange,
            "/mal/anime/collection-job",
            mediaTypes,
            { params -> params.containsKey("year") && params.containsKey("season") }) {
            val year = parameters(exchange)["year"]!!.toInt()
            val season = parameters(exchange)["season"]!!

            animeClient.collectByYearAndSeason(year, season)
            pollClient.collectByYearAndSeason(year, season)
            ""
        } || post(
            exchange,
            "/mal/anime/collection-job",
            mediaTypes,
            { params -> params.containsKey("id") }) {
            val id = parameters(exchange)["id"]!!.toInt()

            animeClient.collectById(id)
            pollClient.collectById(id)
            ""
        }
    }
}