package com.hogimn.malchart.anime

import com.fasterxml.jackson.databind.ObjectMapper
import com.hogimn.malchart.restsupport.BasicController
import com.sun.net.httpserver.HttpExchange

class AnimeController(val mapper: ObjectMapper, val gateway: AnimeDataGateway, val pollClient: PollClient) :
    BasicController() {
    override fun handle(exchange: HttpExchange): Boolean {
        val mediaTypes = listOf("application/json", "application/vnd.malchart.v1+json")

        return get(
            exchange, "/anime", mediaTypes,
            { params -> params.containsKey("id") }) {
            val id = parameters(exchange)["id"]!!
            val record = gateway.findObject(id.toInt())
            if (record != null) {
                mapper.writeValueAsString(record.toAnimeInfo("anime info"))
            } else {
                throw IllegalStateException("Anime with id $id not found")
            }
        } || get(
            exchange, "/anime", mediaTypes,
            { params -> params.containsKey("year") && params.containsKey("season") }) {
            val year = parameters(exchange)["year"]!!.toInt()
            val season = parameters(exchange)["season"]!!
            val records = gateway.findByYearAndSeason(year, season)
            val animeInfoList = records.map { it.toAnimeInfo("anime info") }
            mapper.writeValueAsString(animeInfoList)
        } || get(
            exchange, "/anime/no-poll", mediaTypes,
        ) {
            val pollContentIds = pollClient.fetchPollContentIds("anime")
            val activeRecords = gateway.findActiveAnimes()
            val filteredInfoList = activeRecords
                .filter { record -> !pollContentIds.contains(record.id) }
                .map { it.toAnimeInfo("no poll anime") }

            mapper.writeValueAsString(filteredInfoList)
        } || put(exchange, "/anime", mediaTypes) {
            val request = mapper.readValue(body(exchange), AnimeInfo::class.java)

            val updatedRows = gateway.update(
                id = request.id,
                title = request.title,
                link = request.link,
                image = request.image,
                score = request.score,
                members = request.members,
                genre = request.genre,
                studios = request.studios,
                source = request.source,
                season = request.season,
                year = request.year,
                rank = request.rank,
                popularity = request.popularity,
                scoringCount = request.scoringCount,
                episodes = request.episodes,
                airStatus = request.airStatus,
                type = request.type,
                startDate = request.startDate,
                endDate = request.endDate,
                englishTitle = request.englishTitle,
                japaneseTitle = request.japaneseTitle,
                synopsis = request.synopsis,
                largeImage = request.largeImage,
                rating = request.rating,
                nsfw = request.nsfw
            )

            if (updatedRows > 0) {
                val record = gateway.findObject(request.id)!!
                mapper.writeValueAsString(record.toAnimeInfo("anime updated"))
            } else {
                throw IllegalStateException("Anime with id ${request.id} not found to update")
            }
        } || post(exchange, "/anime", mediaTypes) {
            val inputData = mapper.readValue(body(exchange), AnimeInfo::class.java)

            val newRecord = gateway.create(
                id = inputData.id,
                title = inputData.title,
                link = inputData.link,
                image = inputData.image,
                score = inputData.score,
                members = inputData.members,
                genre = inputData.genre,
                studios = inputData.studios,
                source = inputData.source,
                season = inputData.season,
                year = inputData.year,
                rank = inputData.rank,
                popularity = inputData.popularity,
                scoringCount = inputData.scoringCount,
                episodes = inputData.episodes,
                airStatus = inputData.airStatus,
                type = inputData.type,
                startDate = inputData.startDate,
                endDate = inputData.endDate,
                englishTitle = inputData.englishTitle,
                japaneseTitle = inputData.japaneseTitle,
                synopsis = inputData.synopsis,
                largeImage = inputData.largeImage,
                rating = inputData.rating,
                nsfw = inputData.nsfw
            )

            mapper.writeValueAsString(newRecord.toAnimeInfo("anime created"))
        } || post(exchange, "/anime/upsert", mediaTypes) {
            val inputData = mapper.readValue(body(exchange), AnimeInfo::class.java)

            val newRecord = gateway.upsert(
                id = inputData.id,
                title = inputData.title,
                link = inputData.link,
                image = inputData.image,
                score = inputData.score,
                members = inputData.members,
                genre = inputData.genre,
                studios = inputData.studios,
                source = inputData.source,
                season = inputData.season,
                year = inputData.year,
                rank = inputData.rank,
                popularity = inputData.popularity,
                scoringCount = inputData.scoringCount,
                episodes = inputData.episodes,
                airStatus = inputData.airStatus,
                type = inputData.type,
                startDate = inputData.startDate,
                endDate = inputData.endDate,
                englishTitle = inputData.englishTitle,
                japaneseTitle = inputData.japaneseTitle,
                synopsis = inputData.synopsis,
                largeImage = inputData.largeImage,
                rating = inputData.rating,
                nsfw = inputData.nsfw
            )

            mapper.writeValueAsString(newRecord.toAnimeInfo("anime upserted"))
        }
    }

    private fun AnimeRecord.toAnimeInfo(info: String): AnimeInfo {
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
            info
        )
    }
}