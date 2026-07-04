package com.hogimn.malchart.mal

import com.sun.net.httpserver.HttpExchange
import com.hogimn.malchart.restsupport.BasicController

class MalController(
    val animeClient: AnimeClient,
    val pollClient: AnimePollClient
) : BasicController() {
    override fun handle(exchange: HttpExchange): Boolean {
        val mediaTypes = listOf("application/json", "application/vnd.malchart.v1+json")

        return post(exchange, "/mal/anime/by-year-and-season", mediaTypes) {
            val year = parameters(exchange)["year"]!!.toInt()
            val season = parameters(exchange)["season"]!!
            animeClient.collectByYearAndSeason(year, season)
            ""
        } || post(exchange, "/mal/anime/poll/by-year-and-season", mediaTypes) {
            val year = parameters(exchange)["year"]!!.toInt()
            val season = parameters(exchange)["season"]!!
            pollClient.collectByYearAndSeason(year, season)
            ""
        }
    }
}