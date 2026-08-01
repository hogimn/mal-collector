package com.hogimn.malcollector.mal

import AppLoggerFactory
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.hogimn.malcollector.circuitbreakersupport.CircuitBreaker
import com.hogimn.malcollector.discoverysupport.DiscoveryClient
import com.hogimn.malcollector.malsupport.MalProvider
import com.hogimn.malcollector.restsupport.RestTemplate
import dev.katsute.mal4j.forum.ForumTopic
import java.lang.Thread.sleep

class AnimePollClient(val mapper: ObjectMapper, val template: RestTemplate, val malProvider: MalProvider) {
    private val logger = AppLoggerFactory.getLogger(javaClass)
    private val circuitBreaker = CircuitBreaker()

    companion object {
        private const val API_DELAY_MS = 2000L
        private const val FORUM_PAGE_LIMIT = 100
        private const val CONTENT_TYPE_ANIME = "anime"
        private const val SERVICE_POLL = "poll"
        private const val SERVICE_ANIME = "anime"

        val pollOptionMap: Map<String, Int> = mapOf(
            "5 out of 5: Loved it!" to 5,
            "4 out of 5: Liked it" to 4,
            "3 out of 5: It was OK" to 3,
            "2 out of 5: Disliked it" to 2,
            "1 out of 5: Hated it" to 1
        )
    }

    fun collectByYearAndSeason(year: Int, season: String) {
        logger.info("Start collecting anime poll $year/$season")

        val animeList = findSeasonalAnime(year, season) ?: return

        logger.info("Found ${animeList.size} anime titles for $year/$season")

        animeList.forEachIndexed { index, anime ->
            logger.info("[${index + 1}/${animeList.size}] Collecting poll for: ${anime.title}")
            collectPoll(anime)
        }

        logger.info("End collecting anime poll $year/$season")
    }

    fun collectById(id: Int) {
        logger.info("Start collecting anime $id")
        val anime = findAnime(id) ?: return
        collectPoll(anime)
    }

    fun collectByIds(ids: List<Int>) {
        logger.info("Start collecting anime poll for ${ids.size} IDs")

        ids.forEachIndexed { index, id ->
            logger.info("[${index + 1}/${ids.size}] Collecting poll for anime ID: $id")

            val anime = findAnime(id)
            if (anime == null) {
                logger.warn("Anime not found for ID: $id. Skipping poll collection.")
                return@forEachIndexed
            }

            try {
                collectPoll(anime)
            } catch (e: Exception) {
                logger.error("Failed to collect poll for anime ID $id. Details: {}", e.message, e)
            }
        }

        logger.info("End collecting anime poll for ${ids.size} IDs")
    }

    private fun collectPoll(anime: AnimeInfo) {
        val searchKeyword = anime.title + " Poll Episode Discussion"

        sleep(API_DELAY_MS)
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

            val episode = getEpisodeFromTopicTitle(forumTopic.title)
            if (episode == -1) {
                logger.error("Failed to get episode from topic title: {}", forumTopic.title)
                continue
            }

            upsertPoll(forumTopic.id, episode, anime.id)
        }
    }

    private fun upsertPoll(topicId: Long, episode: Int, animeId: Int) {
        val voteZeroOptions = mutableSetOf(1, 2, 3, 4, 5)

        sleep(API_DELAY_MS)
        val forumTopicDetail = malProvider.getMyAnimeList().getForumTopicDetail(topicId)

        val poll = forumTopicDetail.poll
        val topicTitle = forumTopicDetail.title
        val options = poll.options

        if (options == null) {
            logger.info("There is no poll options (null)")
            return
        }

        for (option in options) {
            try {
                val pollOptionId = pollOptionMap[option.text] ?: continue
                voteZeroOptions.remove(pollOptionId)

                val pollInfo = createPollInfo(animeId, topicId, pollOptionId, topicTitle, episode, option.votes)
                logger.info(pollInfo.toString())
                sendPollRequest(pollInfo)

            } catch (e: Exception) {
                logger.error(e.message, e)
            }
        }

        voteZeroOptions.forEach { pollOptionId ->
            try {
                val pollInfo = createPollInfo(animeId, topicId, pollOptionId, topicTitle, episode, 0)
                sendPollRequest(pollInfo)
            } catch (e: Exception) {
                logger.error(e.message, e)
            }
        }
    }

    private fun createPollInfo(
        animeId: Int,
        topicId: Long,
        pollOptionId: Int,
        topicTitle: String?,
        episode: Int,
        votes: Int
    ): PollInfo {
        return PollInfo(
            contentId = animeId,
            contentType = CONTENT_TYPE_ANIME,
            topicId = topicId.toInt(),
            pollOptionId = pollOptionId,
            title = topicTitle ?: "",
            episode = episode,
            votes = votes
        )
    }

    private fun sendPollRequest(pollInfo: PollInfo) {
        val endpoint = DiscoveryClient(mapper, template).getUrl(SERVICE_POLL)
        circuitBreaker.withCircuitBreaker({
            template.post(
                "$endpoint/poll/upsert", "application/json",
                mapper.writeValueAsString(pollInfo)
            )
        }, fallback())
    }

    private fun fetchForumTopics(keyword: String): List<ForumTopic> {
        val forumTopics = mutableListOf<ForumTopic>()
        var offset = 0

        while (true) {
            sleep(API_DELAY_MS)
            val tempForumTopics = malProvider
                .getMyAnimeList()
                .forumTopics
                .withQuery(keyword)
                .withLimit(FORUM_PAGE_LIMIT)
                .withOffset(offset)
                .search()

            forumTopics.addAll(tempForumTopics)
            logger.info("offset: {}, limit: {}, size of list: {}", offset, FORUM_PAGE_LIMIT, forumTopics.size)

            if (tempForumTopics.size >= FORUM_PAGE_LIMIT) {
                offset += FORUM_PAGE_LIMIT
            } else {
                break
            }
        }

        return forumTopics
    }

    private fun findSeasonalAnime(year: Int, season: String): List<AnimeInfo>? {
        val params = listOf(Pair("year", year.toString()), Pair("season", season))
        return fetchAnimeData(params, object : TypeReference<List<AnimeInfo>>() {})
    }

    private fun findAnime(id: Int): AnimeInfo? {
        val params = listOf(Pair("id", id.toString()))
        return fetchAnimeData(params, object : TypeReference<AnimeInfo>() {})
    }

    private fun <T> fetchAnimeData(params: List<Pair<String, String>>, typeReference: TypeReference<T>): T? {
        val endpoint = DiscoveryClient(mapper, template).getUrl(SERVICE_ANIME)
        return circuitBreaker.withCircuitBreaker({
            val response = template.get(
                "$endpoint/anime", "application/json", *params.toTypedArray()
            )
            mapper.readValue(response, typeReference)
        }, fallback())
    }

    private fun getEpisodeFromTopicTitle(topicTitle: String): Int {
        val regex = "Episode (\\d+)".toRegex()

        return try {
            val matchResult = regex.find(topicTitle)
            matchResult?.groupValues?.get(1)?.toInt() ?: run {
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

    private fun fallback(): () -> Nothing? = { null }
}