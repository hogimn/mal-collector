package com.hogimn.malchart.mal

import com.sun.net.httpserver.HttpExchange
import com.hogimn.malchart.restsupport.BasicController

class MalController(val animeService: AnimeCollectClient) : BasicController() {
    override fun handle(exchange: HttpExchange): Boolean {
        val mediaTypes = listOf("application/json", "application/vnd.malchart.v1+json")

        return post(exchange, "/mal/by-year-and-season", mediaTypes) {
            val year = parameters(exchange)["year"]!!.toInt()
            val season = parameters(exchange)["season"]!!
            animeService.collectByYearAndSeason(year, season)
            ""
        }
    }
}