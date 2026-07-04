package com.hogimn.malchart.mal

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.hogimn.malchart.circuitbreaker.CircuitBreaker
import com.hogimn.malchart.discovery.DiscoveryClient
import com.hogimn.malchart.malsupport.MalProvider
import com.hogimn.malchart.restsupport.RestTemplate
import dev.katsute.mal4j.forum.ForumTopic
import java.lang.Thread.sleep

class AnimePollClient(val mapper: ObjectMapper, val template: RestTemplate, val malProvider: MalProvider) {
    private val logger = AppLoggerFactory.getLogger(javaClass)
    private val circuitBreaker = CircuitBreaker()

    val pollOptionMap: Map<String, Int> = mapOf(
        "5 out of 5: Loved it!" to 5,
        "4 out of 5: Liked it" to 4,
        "3 out of 5: It was OK" to 3,
        "2 out of 5: Disliked it" to 2,
        "1 out of 5: Hated it" to 1
    )

    fun collectByYearAndSeason(year: Int, season: String) {
        logger.info("Start collecting anime poll $year/$season")

        val animeList = findSeasonalAnime(year, season) ?: return

        logger.info("Found ${animeList.size} anime titles for $year/$season")

        animeList.forEachIndexed { index, anime ->
            logger.info("[${index + 1}/${animeList.size}] Collecting poll for: ${anime.title}")

            sleep(2000)
            collectPoll(anime)
        }

        logger.info("End collecting anime poll $year/$season")
    }

    private fun collectPoll(anime: AnimeInfo) {
        val searchKeyword = anime.title + " Poll Episode Discussion"
        val forumTopics = fetchForumTopics(searchKeyword)
        for (forumTopic in forumTopics) {
            if (!isFirstWordMatching(forumTopic.title, anime.title)) {
                logger.info(
                    "Topic title does not start with anime title first word, or vice versa. topic: {},  anime: {}",
                    forumTopic.title, anime.title
                )
                continue
            }

            if (!forumTopic.title.endsWith("Discussion")) {
                logger.info("Topic name does not end with Discussion. {}", forumTopic.title)
                continue
            }

            if (checkMangaTopic(forumTopic.title)) {
                logger.info("Topic name is manga discussion. topic: {},  anime: {}", forumTopic.title, anime.title)
                continue
            }

            if (!checkTitleSame(forumTopic.title, anime.title)) {
                logger.info(
                    "Topic name is far different from anime name. topic: {},  anime: {}",
                    forumTopic.title, anime.title
                )
                continue
            }

            val episode = getEpisodeFromTopicTitle(forumTopic.title);
            if (episode == -1) {
                logger.error("Failed to get episode from topic title: {}", forumTopic.title);
                continue
            }

            upsertPoll(forumTopic.id, episode, anime.id)
        }
    }

    private fun upsertPoll(topicId: Long, episode: Int, animeId: Int) {
        val endpoint = DiscoveryClient(mapper, template).getUrl("poll")
        val voteZeroOptions = mutableSetOf(1, 2, 3, 4, 5)

        val forumTopicDetail = try {
            malProvider.getMyAnimeList().getForumTopicDetail(topicId)
        } catch (e: Exception) {
            logger.error(
                "getForumTopicDetail Failed: Anime Id: {}, Episode: {}, Topic Id:{}, Error Message: {}",
                animeId, episode, topicId, e.message, e
            )
            return
        }

        val poll = forumTopicDetail.poll
        val topicTitle = forumTopicDetail.title
        val options = poll.options

        if (options == null) {
            logger.info("There is no poll options (null)")
            return
        }

        for (option in options) {
            try {
                val votes = option.votes
                val text = option.text
                val pollOptionId = pollOptionMap[text]

                voteZeroOptions.remove(pollOptionId)

                val pollInfo = PollInfo(
                    contentId = animeId,
                    contentType = "anime",
                    topicId = topicId.toInt(),
                    pollOptionId = pollOptionId!!,
                    title = topicTitle,
                    episode = episode,
                    votes = votes
                )

                logger.info(pollInfo.toString())

                circuitBreaker.withCircuitBreaker({
                    template.post(
                        "$endpoint/poll/upsert", "application/json",
                        mapper.writeValueAsString(pollInfo)
                    )
                }, fallback())

            } catch (e: Exception) {
                logger.error(e.message, e)
            }
        }

        voteZeroOptions.forEach { pollOptionId ->
            try {
                val pollInfo = PollInfo(
                    contentId = animeId,
                    contentType = "anime",
                    topicId = topicId.toInt(),
                    pollOptionId = pollOptionId,
                    title = topicTitle,
                    episode = episode,
                    votes = 0
                )

                circuitBreaker.withCircuitBreaker({
                    template.post(
                        "$endpoint/poll/upsert", "application/json",
                        mapper.writeValueAsString(pollInfo)
                    )
                }, fallback())
            } catch (e: Exception) {
                logger.error(e.message, e)
            }
        }
    }

    private fun getEpisodeFromTopicTitle(topicTitle: String): Int {
        val regex = "Episode (\\d+)".toRegex()

        return try {
            val matchResult = regex.find(topicTitle)

            if (matchResult != null) {
                val episodeNumber = matchResult.groupValues[1]
                episodeNumber.toInt()
            } else {
                println("No episode number found in: $topicTitle")
                -1
            }
        } catch (e: Exception) {
            logger.error(e.message, e)
            -1
        }
    }

    private fun checkTitleSame(topicTitle: String?, animeTitle: String?): Boolean {
        if (topicTitle.isNullOrEmpty() || animeTitle.isNullOrEmpty()) {
            return false
        }

        val regexChars = "[\\[\\]\". :;\\-!?]".toRegex()
        val regexTv = "\\(TV\\)".toRegex()

        var cleanTopic = topicTitle.lowercase().replace(regexChars, "").replace(regexTv, "")
        val cleanAnime = animeTitle.lowercase().replace(regexChars, "").replace(regexTv, "")

        val indexOfEpisode = cleanTopic.lastIndexOf("episode")
        if (indexOfEpisode == -1) {
            return false
        }

        cleanTopic = cleanTopic.substring(0, indexOfEpisode)

        return cleanTopic == cleanAnime
    }

    private fun checkMangaTopic(topicTitle: String): Boolean {
        val regex = "Chapter \\d+ Discussion".toRegex()
        return regex.containsMatchIn(topicTitle)
    }

    private fun isFirstWordMatching(topicTitle: String?, animeTitle: String?): Boolean {
        if (topicTitle.isNullOrEmpty() || animeTitle.isNullOrEmpty()) {
            return false
        }

        val regex = "[\\[\\]\".:;\\-!?]".toRegex()

        val cleanTopic = topicTitle.lowercase().replace(regex, "")
        val cleanAnime = animeTitle.lowercase().replace(regex, "")

        val topicFirstWord = cleanTopic.split("\\s".toRegex(), limit = 2).firstOrNull() ?: ""
        val animeFirstWord = cleanAnime.split("\\s".toRegex(), limit = 2).firstOrNull() ?: ""

        if (topicFirstWord.isEmpty() || animeFirstWord.isEmpty()) {
            return false
        }

        return cleanTopic.startsWith(animeFirstWord) || cleanAnime.startsWith(topicFirstWord)
    }

    private fun fetchForumTopics(keyword: String): List<ForumTopic> {
        val forumTopics = mutableListOf<ForumTopic>()
        var offset = 0
        val limit = 100

        while (true) {
            val tempForumTopics = malProvider
                .getMyAnimeList()
                .forumTopics
                .withQuery(keyword)
                .withLimit(limit)
                .withOffset(offset)
                .search()

            forumTopics.addAll(tempForumTopics)

            logger.info("offset: {}, limit: {}, size of list: {}", offset, limit, forumTopics.size)

            if (tempForumTopics.size >= limit) {
                offset += limit
            } else {
                break
            }

            sleep(2000)
        }

        return forumTopics
    }

    private fun findSeasonalAnime(year: Int, season: String): List<AnimeInfo>? {
        val endpoint = DiscoveryClient(mapper, template).getUrl("anime")

        return circuitBreaker.withCircuitBreaker({
            val params = listOf(
                Pair("year", year.toString()),
                Pair("season", season)
            )
            val response = template.get(
                "$endpoint/anime/by-year-and-season", "application/json", *params.toTypedArray()
            )

            mapper.readValue(response, object : TypeReference<List<AnimeInfo>>() {})
        }, fallback())
    }

    private fun fallback(): () -> Nothing? = { null }
}