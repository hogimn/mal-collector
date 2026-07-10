package com.hogimn.malcollector.mal

import AppLoggerFactory
import com.hogimn.malcollector.restsupport.BasicController
import com.sun.net.httpserver.HttpExchange
import java.time.LocalDate
import java.time.Month
import java.util.concurrent.Executors

class MalController(
    val animeClient: AnimeClient, val pollClient: AnimePollClient
) : BasicController() {

    private val logger = AppLoggerFactory.getLogger(javaClass)
    private val collectionExecutor = Executors.newSingleThreadExecutor()

    override fun handle(exchange: HttpExchange): Boolean {
        val mediaTypes = listOf("application/json", "application/vnd.malcollector.v1+json")

        return post(
            exchange,
            "/mal/anime/collection-job",
            mediaTypes,
            { params -> params.containsKey("year") && params.containsKey("season") }) {
            val year = parameters(exchange)["year"]!!.toInt()
            val season = parameters(exchange)["season"]!!

            collectionExecutor.submit {
                try {
                    animeClient.collectByYearAndSeason(year, season)
                    pollClient.collectByYearAndSeason(year, season)
                } catch (e: Exception) {
                    logger.error("Failed to collect data for $year $season", e)
                }
            }

            "Collection job for $year $season has been successfully scheduled in the background."
        } || post(
            exchange,
            "/mal/anime/collection-job",
            mediaTypes,
            { params -> params.containsKey("id") }) {
            val id = parameters(exchange)["id"]!!.toInt()

            collectionExecutor.submit {
                try {
                    animeClient.collectById(id)
                    pollClient.collectById(id)
                } catch (e: Exception) {
                    logger.error("Failed to collect data for ID $id", e)
                }
            }

            "Collection job for Anime ID $id has been successfully scheduled in the background."
        } || post(
            exchange,
            "/mal/anime/collection-job/archive",
            mediaTypes,
            { true }
        ) {
            collectionExecutor.submit {
                collectAllAnimeSince2000()
            }

            "Grand archive collection job (from current year down to 2000) has been successfully started."
        }
    }

    private fun collectAllAnimeSince2000() {
        val now = LocalDate.now()
        val currentYear = now.year
        val currentSeason = convertMonthToSeason(now.month)

        val seasons = listOf("winter", "spring", "summer", "fall")

        logger.info("Starting grand collection job from $currentYear down to 2000")

        for (year in currentYear downTo 2000) {
            val startSeasonIndex = if (year == currentYear) seasons.indexOf(currentSeason) else 3

            for (i in startSeasonIndex downTo 0) {
                val season = seasons[i]

                try {
                    logger.info("Archiving: $year $season...")
                    animeClient.collectByYearAndSeason(year, season)
                    pollClient.collectByYearAndSeason(year, season)
                } catch (e: Exception) {
                    logger.error("Failed to collect data for $year $season", e)
                }
            }
        }
        logger.info("Grand collection job finished successfully!")
    }

    private fun convertMonthToSeason(month: Month): String {
        return when (month) {
            Month.JANUARY, Month.FEBRUARY, Month.MARCH -> "winter"
            Month.APRIL, Month.MAY, Month.JUNE -> "spring"
            Month.JULY, Month.AUGUST, Month.SEPTEMBER -> "summer"
            Month.OCTOBER, Month.NOVEMBER, Month.DECEMBER -> "fall"
        }
    }
}