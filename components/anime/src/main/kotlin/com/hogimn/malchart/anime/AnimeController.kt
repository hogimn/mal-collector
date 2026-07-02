package com.hogimn.malchart.anime

import com.fasterxml.jackson.databind.ObjectMapper
import com.sun.net.httpserver.HttpExchange
import com.hogimn.malchart.restsupport.BasicController

class AnimeController(val mapper: ObjectMapper, val gateway: AnimeDataGateway) : BasicController() {
    override fun handle(exchange: HttpExchange): Boolean {
        return get(exchange, "/anime", listOf("application/json", "application/vnd.malchart.v1+json")) {
            val animeId = parameters(exchange)["animeId"]!!
            val record = gateway.findObject(animeId.toInt())
            if (record != null) {
                mapper.writeValueAsString(record.toAnimeInfo())
            } else {
                throw IllegalStateException("Anime with id $animeId not found")
            }
        } || get(
            exchange,
            "/anime/by-year-and-season",
            listOf("application/json", "application/vnd.malchart.v1+json")
        ) {
            val year = parameters(exchange)["year"]!!.toInt()
            val season = parameters(exchange)["season"]!!
            val records = gateway.findByYearAndSeason(year, season)
            val animeInfoList = records.map { it.toAnimeInfo() }
            mapper.writeValueAsString(animeInfoList)
        } || put(
            exchange,
            "/anime",
            listOf("application/json", "application/vnd.malchart.v1+json")
        ) {
            val updateData = mapper.readValue(body(exchange), AnimeInfo::class.java)

            val updatedRows = gateway.update(
                id = updateData.id,
                title = updateData.title,
                link = updateData.link,
                image = updateData.image,
                score = updateData.score,
                members = updateData.members,
                genre = updateData.genre,
                studios = updateData.studios,
                source = updateData.source,
                season = updateData.season,
                year = updateData.year,
                rank = updateData.rank,
                popularity = updateData.popularity,
                scoringCount = updateData.scoringCount,
                episodes = updateData.episodes,
                airStatus = updateData.airStatus,
                type = updateData.type,
                startDate = updateData.startDate,
                endDate = updateData.endDate,
                englishTitle = updateData.englishTitle,
                japaneseTitle = updateData.japaneseTitle,
                synopsis = updateData.synopsis,
                createdAt = updateData.createdAt,
                updatedAt = java.time.LocalDateTime.now(),
                largeImage = updateData.largeImage,
                rating = updateData.rating,
                nsfw = updateData.nsfw
            )

            if (updatedRows > 0) {
                """{"result": "success", "updatedRows": $updatedRows}"""
            } else {
                throw IllegalStateException("Anime with id ${updateData.id} not found to update")
            }
        }
    }

    private fun AnimeRecord.toAnimeInfo(): AnimeInfo {
        return AnimeInfo(
            id = this.id,
            title = this.title,
            link = this.link,
            image = this.image,
            score = this.score,
            members = this.members,
            genre = this.genre,
            studios = this.studios,
            source = this.source,
            season = this.season,
            year = this.year,
            rank = this.rank,
            popularity = this.popularity,
            scoringCount = this.scoringCount,
            episodes = this.episodes,
            airStatus = this.airStatus,
            type = this.type,
            startDate = this.startDate,
            endDate = this.endDate,
            englishTitle = this.englishTitle,
            japaneseTitle = this.japaneseTitle,
            synopsis = this.synopsis,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt,
            largeImage = this.largeImage,
            rating = this.rating,
            nsfw = this.nsfw,
            "anime info"
        )
    }
}