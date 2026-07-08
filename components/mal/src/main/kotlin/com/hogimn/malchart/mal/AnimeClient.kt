package com.hogimn.malchart.mal

import AppLoggerFactory
import com.fasterxml.jackson.databind.ObjectMapper
import com.hogimn.malchart.circuitbreaker.CircuitBreaker
import com.hogimn.malchart.discovery.DiscoveryClient
import com.hogimn.malchart.malsupport.MalProvider
import com.hogimn.malchart.restsupport.RestTemplate
import dev.katsute.mal4j.anime.Anime
import dev.katsute.mal4j.anime.property.time.Season
import java.lang.Thread.sleep
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

class AnimeClient(val mapper: ObjectMapper, val template: RestTemplate, val malProvider: MalProvider) {
    private val logger = AppLoggerFactory.getLogger(javaClass)
    private val circuitBreaker = CircuitBreaker()

    fun collectByYearAndSeason(year: Int, season: String) {
        logger.info("Start collecting $year/$season")

        val endpoint = DiscoveryClient(mapper, template).getUrl("anime")
        val animeList = findSeasonalAnime(year, season)
        animeList.forEachIndexed { index, anime ->
            logger.info("[${index + 1}/${animeList.size}] Collecting anime for: ${anime.title}")

            sleep(2000)

            try {
                val startSeason = anime.startSeason
                if (startSeason == null ||
                    startSeason.year != year ||
                    startSeason.season?.field() != season
                ) {
                    logger.info(
                        "Skipping anime '{}': Year {} (expected: {}), Season {} (expected: {})",
                        anime.title, anime.startSeason.year, year,
                        anime.startSeason.season.field(), season
                    )
                    return@forEachIndexed
                }

                val animeInfo = anime.toAnimeInfo()
                circuitBreaker.withCircuitBreaker({
                    template.post(
                        "$endpoint/anime/upsert", "application/json",
                        mapper.writeValueAsString(animeInfo)
                    )
                }, fallback())

            } catch (e: Exception) {
                logger.error("Error processing anime. Skipping to the next item. Details: {}", e.message, e)
            }
        }

        logger.info("End collecting $year/$season")
    }

    private fun fallback(): () -> Nothing? = { null }

    private fun findSeasonalAnime(year: Int, season: String): List<Anime> {
        var offset = 0
        val limit = 500
        val animeList = mutableListOf<Anime>()

        do {
            sleep(2000)
            val tempAnimeList = malProvider
                .getMyAnimeList()
                .getAnimeSeason(year, Season.asEnum(season))
                .withLimit(limit)
                .withOffset(offset)
                .includeNSFW()
                .search()

            animeList.addAll(tempAnimeList)

            if (tempAnimeList.size >= limit) {
                offset += limit
            }
        } while (tempAnimeList.size >= limit)

        return animeList
    }

    fun Anime.toAnimeInfo(): AnimeInfo {
        val picture = this.mainPicture
        val mediumUrl = picture?.mediumURL ?: ""
        val largeUrl = picture?.largeURL ?: ""

        val startSeason = this.startSeason
        val year = startSeason?.year ?: 0
        val season = startSeason?.season?.field() ?: ""

        val genreNames = this.genres
            ?.mapNotNull { it?.name }
            ?.joinToString(", ") ?: ""

        val studioNames = this.studios
            ?.mapNotNull { it?.name }
            ?.joinToString(", ") ?: ""

        val altTitles = this.alternativeTitles

        return AnimeInfo(
            id = this.id.toInt(),
            title = this.title ?: "",
            link = "https://myanimelist.net/anime/${this.id}",
            image = mediumUrl,
            largeImage = largeUrl,
            score = this.meanRating?.toDouble() ?: 0.0,
            members = this.userListingCount,
            genre = genreNames,
            studios = studioNames,
            source = this.source?.field() ?: this.rawSource ?: "",
            year = year,
            season = season,
            rank = this.rank ?: -1,
            popularity = this.popularity,
            scoringCount = this.userScoringCount,
            episodes = this.episodes,
            airStatus = this.status?.field() ?: this.rawStatus ?: "",
            type = this.type?.field() ?: this.rawType ?: "",
            startDate = this.startDate?.date?.toUtcLocalDateTime() ?: LocalDateTime.now(),
            endDate = this.endDate?.date?.toUtcLocalDateTime() ?: LocalDateTime.now(),
            englishTitle = altTitles?.english ?: "",
            japaneseTitle = altTitles?.japanese ?: "",
            synopsis = this.synopsis ?: "",
            rating = this.rating?.field() ?: this.rawRating ?: "",
            nsfw = this.nsfw?.field() ?: this.rawNSFW ?: "",
            info = null
        )
    }

    fun Date.toUtcLocalDateTime(zoneId: ZoneId = ZoneId.of("UTC")): LocalDateTime {
        return this.toInstant().atZone(zoneId).toLocalDateTime()
    }
}