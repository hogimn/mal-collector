package com.hogimn.malcollector.mal

import AppLoggerFactory
import com.fasterxml.jackson.databind.ObjectMapper
import com.hogimn.malcollector.circuitbreakersupport.CircuitBreaker
import com.hogimn.malcollector.discoverysupport.DiscoveryClient
import com.hogimn.malcollector.malsupport.MalProvider
import com.hogimn.malcollector.restsupport.RestTemplate
import dev.katsute.mal4j.anime.Anime
import dev.katsute.mal4j.anime.property.time.Season
import java.lang.Thread.sleep
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

class AnimeClient(
    val mapper: ObjectMapper,
    val template: RestTemplate,
    val malProvider: MalProvider
) {
    private val logger = AppLoggerFactory.getLogger(javaClass)
    private val circuitBreaker = CircuitBreaker()

    companion object {
        private const val API_DELAY_MS = 2000L
        private const val PAGE_LIMIT = 500
        private const val SERVICE_NAME = "anime"
    }

    fun collectByYearAndSeason(year: Int, season: String) {
        logger.info("Start collecting $year/$season")

        val animeList = findSeasonAnime(year, season)
        animeList.forEachIndexed { index, anime ->
            logger.info("[${index + 1}/${animeList.size}] Collecting anime for: ${anime.title}")

            sleep(API_DELAY_MS)

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

                if (!isValidAnime(anime)) {
                    return@forEachIndexed
                }

                upsertAnime(anime)

            } catch (e: Exception) {
                logger.error("Error processing anime. Skipping to the next item. Details: {}", e.message, e)
            }
        }

        logger.info("End collecting $year/$season")
    }

    fun collectById(id: Int) {
        val anime = malProvider.getMyAnimeList().getAnime(id.toLong())

        if (!isValidAnime(anime)) {
            return
        }

        upsertAnime(anime)
    }

    fun collectByIds(ids: List<Int>) {
        logger.info("Start collecting anime for ${ids.size} IDs")

        ids.forEachIndexed { index, id ->
            logger.info("[${index + 1}/${ids.size}] Collecting anime for ID: $id")

            sleep(API_DELAY_MS)

            try {
                val anime = malProvider.getMyAnimeList().getAnime(id.toLong())

                if (!isValidAnime(anime)) {
                    return@forEachIndexed
                }

                upsertAnime(anime)
            } catch (e: Exception) {
                logger.error("Failed to collect anime for ID $id. Details: {}", e.message, e)
            }
        }

        logger.info("End collecting anime for ${ids.size} IDs")
    }

    private fun isValidAnime(anime: Anime): Boolean {
        if (anime.type.field() != "tv") {
            logger.info("Skipping anime '{}': Type {} (expected: tv)", anime.title, anime.type.field())
            return false
        }

        val rating = anime.meanRating?.toInt() ?: 0
        if (rating == 0) {
            logger.info("Skipping anime '{}': Mean rating is null or 0 (expected: > 0)", anime.title)
            return false
        }

        return true
    }

    private fun upsertAnime(anime: Anime) {
        val animeInfo = anime.toAnimeInfo()
        val endpoint = DiscoveryClient(mapper, template).getUrl(SERVICE_NAME)

        circuitBreaker.withCircuitBreaker({
            template.post(
                "$endpoint/anime/upsert",
                "application/json",
                mapper.writeValueAsString(animeInfo)
            )
        }, fallback())
    }

    private fun fallback(): () -> Nothing? = { null }

    private fun findSeasonAnime(year: Int, season: String): List<Anime> {
        var offset = 0
        val animeList = mutableListOf<Anime>()

        do {
            sleep(API_DELAY_MS)
            val tempAnimeList = malProvider
                .getMyAnimeList()
                .getAnimeSeason(year, Season.asEnum(season))
                .withLimit(PAGE_LIMIT)
                .withOffset(offset)
                .includeNSFW()
                .search()

            animeList.addAll(tempAnimeList)

            if (tempAnimeList.size >= PAGE_LIMIT) {
                offset += PAGE_LIMIT
            }
        } while (tempAnimeList.size >= PAGE_LIMIT)

        return animeList
    }
}

fun Anime.toAnimeInfo(): AnimeInfo {
    val picture = this.mainPicture
    val mediumUrl = picture?.mediumURL ?: ""
    val largeUrl = picture?.largeURL ?: ""

    val startSeason = this.startSeason
    val year = startSeason?.year ?: 0
    val season = startSeason?.season?.field() ?: ""

    val genreNames = this.genres?.mapNotNull { it?.name }?.joinToString(", ") ?: ""
    val studioNames = this.studios?.mapNotNull { it?.name }?.joinToString(", ") ?: ""
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