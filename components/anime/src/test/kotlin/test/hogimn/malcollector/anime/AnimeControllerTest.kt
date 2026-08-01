package test.hogimn.malcollector.anime

import com.fasterxml.jackson.core.type.TypeReference
import com.hogimn.malcollector.anime.AnimeController
import com.hogimn.malcollector.anime.AnimeDataGateway
import com.hogimn.malcollector.anime.AnimeInfo
import com.hogimn.malcollector.anime.PollClient
import com.hogimn.malcollector.jdbcsupport.DataSourceConfig
import com.hogimn.malcollector.jdbcsupport.JdbcTemplate
import com.hogimn.malcollector.restsupport.BasicServer
import com.hogimn.malcollector.restsupport.RestTemplate
import com.hogimn.malcollector.testsupport.TestControllerSupport
import com.hogimn.malcollector.testsupport.TestScenarioSupport
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnimeControllerTest : TestControllerSupport() {
    val dataSource =
        DataSourceConfig().createDataSource("jdbc:mysql://localhost:3306/test_anime?user=uservices&password=uservices")

    private val testPollClient = object : PollClient(mapper, RestTemplate()) {
        override fun fetchPollContentIds(contentType: String): Set<Int> {
            return setOf(1001, 1002)
        }

        override fun fetchEpisodeDistribution(contentId: Int, contentType: String): Map<Int, Map<String, Any>> {
            return mapOf(
                1 to mapOf("count" to 100, "score" to 4.5),
                2 to mapOf("count" to 85, "score" to 4.2)
            )
        }
    }

    private val server = object : BasicServer(8081) {
        override fun registerContexts() {
            context(
                "/anime",
                AnimeController(
                    mapper,
                    AnimeDataGateway(JdbcTemplate(dataSource)),
                    testPollClient
                )
            )
        }
    }

    @Before
    fun setUp() {
        JdbcTemplate(dataSource).apply {
            execute("delete from anime")
        }
        server.start()
    }

    @After
    fun tearDown() {
        server.stop()
    }

    @Test
    fun testNoPoll() {
        TestScenarioSupport(dataSource).loadTestScenario("jacks-test-scenario")

        val pollAnime = AnimeInfo(
            id = 1001,
            title = "Poll Registered Anime",
            link = "https://example.com/anime/1001",
            image = "https://example.com/img/1001.jpg",
            score = 8.0,
            members = 1000,
            genre = "Action",
            studios = "Studio A",
            source = "Original",
            season = "SPRING",
            year = 2026,
            rank = 1,
            popularity = 1,
            scoringCount = 1000,
            episodes = 12,
            airStatus = "Currently Airing",
            type = "TV",
            startDate = LocalDateTime.of(2026, 7, 1, 0, 0, 0),
            endDate = LocalDateTime.of(2026, 9, 23, 0, 0, 0),
            englishTitle = "New Anime English",
            japaneseTitle = "New Anime Japanese",
            synopsis = "This is a synopsis for the newly created anime.",
            largeImage = "https://example.com/img/9999_large.jpg",
            rating = "PG-13",
            nsfw = "SFW",
        )

        val noPollAnime = AnimeInfo(
            id = 9999,
            title = "No Poll Anime",
            link = "https://example.com/anime/9999",
            image = "https://example.com/img/9999.jpg",
            score = 7.5,
            members = 500,
            genre = "Drama",
            studios = "Studio B",
            source = "Manga",
            season = "SPRING",
            year = 2026,
            rank = 2,
            popularity = 2,
            scoringCount = 500,
            episodes = 12,
            airStatus = "Currently Airing",
            type = "TV",
            startDate = LocalDateTime.of(2026, 7, 1, 0, 0, 0),
            endDate = LocalDateTime.of(2026, 9, 23, 0, 0, 0),
            englishTitle = "New Anime English",
            japaneseTitle = "New Anime Japanese",
            synopsis = "This is a synopsis for the newly created anime.",
            largeImage = "https://example.com/img/9999_large.jpg",
            rating = "PG-13",
            nsfw = "SFW",
        )

        template.post("http://localhost:8081/anime", "application/json", mapper.writeValueAsString(pollAnime))
        template.post("http://localhost:8081/anime", "application/json", mapper.writeValueAsString(noPollAnime))

        val response = template.get("http://localhost:8081/anime/no-poll", "application/json")
        val actualNoPollIds: List<Int> = mapper.readValue(response, object : TypeReference<List<Int>>() {})

        assertTrue(actualNoPollIds.contains(4765))
        assertTrue(actualNoPollIds.contains(9999))
        assertTrue(!actualNoPollIds.contains(1001))
        assertTrue(!actualNoPollIds.contains(1002))
    }

    @Test
    fun testFind() {
        TestScenarioSupport(dataSource).loadTestScenario("jacks-test-scenario")

        val id = Pair("id", "4765")
        val response = template.get("http://localhost:8081/anime", "application/json", id)
        val actual: AnimeInfo = mapper.readValue(response, object : TypeReference<AnimeInfo>() {})

        assertEquals(4765, actual.id)
        assertEquals("Attack on Titan", actual.title)
        assertEquals("https://example.com/anime/4765", actual.link)
        assertEquals("https://example.com/img/4765.jpg", actual.image)
        assertEquals(9.12, actual.score)
        assertEquals(2500000, actual.members)
        assertEquals("Action, Fantasy", actual.genre)
        assertEquals("WIT Studio", actual.studios)
        assertEquals("Manga", actual.source)
        assertEquals("SPRING", actual.season)
        assertEquals(2013, actual.year)
        assertEquals(2, actual.rank)
        assertEquals(1, actual.popularity)
        assertEquals(1800000, actual.scoringCount)
        assertEquals(25, actual.episodes)
        assertEquals("Finished Airing", actual.airStatus)
        assertEquals("TV", actual.type)
        assertEquals(LocalDateTime.of(2013, 4, 7, 0, 0, 0), actual.startDate)
        assertEquals(LocalDateTime.of(2013, 9, 29, 0, 0, 0), actual.endDate)
        assertEquals("Attack on Titan", actual.englishTitle)
        assertEquals("Shingeki no Kyojin", actual.japaneseTitle)
        assertEquals(
            "Centuries ago, mankind was slaughtered to near extinction by monstrous humanoid creatures called Titans...",
            actual.synopsis
        )
        assertEquals(LocalDateTime.of(2026, 6, 28, 21, 0, 0), actual.createdAt)
        assertEquals(LocalDateTime.of(2026, 6, 28, 21, 0, 0), actual.updatedAt)
        assertEquals("https://example.com/img/4765_large.jpg", actual.largeImage)
        assertEquals("R - 17+", actual.rating)
        assertEquals("SFW", actual.nsfw)
        assertEquals("anime info", actual.info)
        assertEquals(100, actual.episodeDistribution?.get(1)?.get("count"))
    }

    @Test
    fun testFindByYearAndSeason() {
        TestScenarioSupport(dataSource).loadTestScenario("jacks-test-scenario")

        val params = listOf(
            Pair("year", "2013"),
            Pair("season", "SPRING")
        )

        val response =
            template.get("http://localhost:8081/anime", "application/json", *params.toTypedArray())

        val actual: List<AnimeInfo> =
            mapper.readValue(response, object : TypeReference<List<AnimeInfo>>() {})

        assertEquals(1, actual.size)

        val record = actual[0]
        assertEquals(4765, record.id)
        assertEquals("Attack on Titan", record.title)
        assertEquals("https://example.com/anime/4765", record.link)
        assertEquals("https://example.com/img/4765.jpg", record.image)
        assertEquals(9.12, record.score)
        assertEquals(2500000, record.members)
        assertEquals("Action, Fantasy", record.genre)
        assertEquals("WIT Studio", record.studios)
        assertEquals("Manga", record.source)
        assertEquals("SPRING", record.season)
        assertEquals(2013, record.year)
        assertEquals(2, record.rank)
        assertEquals(1, record.popularity)
        assertEquals(1800000, record.scoringCount)
        assertEquals(25, record.episodes)
        assertEquals("Finished Airing", record.airStatus)
        assertEquals("TV", record.type)
        assertEquals(LocalDateTime.of(2013, 4, 7, 0, 0, 0), record.startDate)
        assertEquals(LocalDateTime.of(2013, 9, 29, 0, 0, 0), record.endDate)
        assertEquals("Attack on Titan", record.englishTitle)
        assertEquals("Shingeki no Kyojin", record.japaneseTitle)
        assertEquals(
            "Centuries ago, mankind was slaughtered to near extinction by monstrous humanoid creatures called Titans...",
            record.synopsis
        )
        assertEquals(LocalDateTime.of(2026, 6, 28, 21, 0, 0), record.createdAt)
        assertEquals(LocalDateTime.of(2026, 6, 28, 21, 0, 0), record.updatedAt)
        assertEquals("https://example.com/img/4765_large.jpg", record.largeImage)
        assertEquals("R - 17+", record.rating)
        assertEquals("SFW", record.nsfw)
        assertEquals("anime info", record.info)
        assertEquals(100, record.episodeDistribution?.get(1)?.get("count"))
    }

    @Test
    fun testUpdate() {
        TestScenarioSupport(dataSource).loadTestScenario("jacks-test-scenario")

        val updateData = AnimeInfo(
            id = 4765,
            title = "Attack on Titan - Updated",
            link = "https://example.com/anime/4765-updated",
            image = "https://example.com/img/4765.jpg",
            score = 9.55,
            members = 2600000,
            genre = "Action, Fantasy, Drama",
            studios = "WIT Studio",
            source = "Manga",
            season = "SPRING",
            year = 2013,
            rank = 1,
            popularity = 1,
            scoringCount = 1900000,
            episodes = 25,
            airStatus = "Finished Airing",
            type = "TV",
            startDate = LocalDateTime.of(2013, 4, 7, 0, 0, 0),
            endDate = LocalDateTime.of(2013, 9, 29, 0, 0, 0),
            englishTitle = "Attack on Titan",
            japaneseTitle = "Shingeki no Kyojin",
            synopsis = "Centuries ago, mankind was slaughtered...",
            createdAt = LocalDateTime.of(2026, 6, 28, 21, 0, 0),
            updatedAt = LocalDateTime.of(2026, 6, 28, 21, 0, 0),
            largeImage = "https://example.com/img/4765_large.jpg",
            rating = "R - 17+",
            nsfw = "SFW"
        )

        val requestBody = mapper.writeValueAsString(updateData)

        val response = template.put("http://localhost:8081/anime", "application/json", requestBody)

        val actual: AnimeInfo = mapper.readValue(response, object : TypeReference<AnimeInfo>() {})

        assertEquals(4765, actual.id)
        assertEquals("Attack on Titan - Updated", actual.title)
        assertEquals(9.55, actual.score)
        assertEquals(1, actual.rank)
        assertEquals("anime updated", actual.info)
        assertEquals(100, actual.episodeDistribution?.get(1)?.get("count"))
    }

    @Test
    fun testCreate() {
        val newAnime = AnimeInfo(
            id = 9999,
            title = "New Anime Title",
            link = "https://example.com/anime/9999",
            image = "https://example.com/img/9999.jpg",
            score = 8.5,
            members = 100000,
            genre = "Sci-Fi, Action",
            studios = "Trigger",
            source = "Original",
            season = "SUMMER",
            year = 2026,
            rank = 150,
            popularity = 300,
            scoringCount = 85000,
            episodes = 12,
            airStatus = "Currently Airing",
            type = "TV",
            startDate = LocalDateTime.of(2026, 7, 1, 0, 0, 0),
            endDate = LocalDateTime.of(2026, 9, 23, 0, 0, 0),
            englishTitle = "New Anime English",
            japaneseTitle = "New Anime Japanese",
            synopsis = "This is a synopsis for the newly created anime.",
            largeImage = "https://example.com/img/9999_large.jpg",
            rating = "PG-13",
            nsfw = "SFW",
        )

        val requestBody = mapper.writeValueAsString(newAnime)

        val response = template.post("http://localhost:8081/anime", "application/json", requestBody)
        val actual: AnimeInfo = mapper.readValue(response, object : TypeReference<AnimeInfo>() {})

        assertEquals(9999, actual.id)
        assertEquals("New Anime Title", actual.title)
        assertEquals("https://example.com/anime/9999", actual.link)
        assertEquals(8.5, actual.score)
        assertEquals("Trigger", actual.studios)
        assertEquals("SUMMER", actual.season)
        assertEquals(2026, actual.year)
        assertEquals("anime created", actual.info)
        assertEquals(100, actual.episodeDistribution?.get(1)?.get("count"))

        val now = LocalDateTime.now()
        assert(actual.createdAt!!.isAfter(now.minusSeconds(5)) && actual.createdAt.isBefore(now.plusSeconds(5)))
        assert(actual.updatedAt!!.isAfter(now.minusSeconds(5)) && actual.updatedAt.isBefore(now.plusSeconds(5)))

        val idParam = Pair("id", "9999")
        val getResponse = template.get("http://localhost:8081/anime", "application/json", idParam)
        val dbActual: AnimeInfo = mapper.readValue(getResponse, object : TypeReference<AnimeInfo>() {})

        assertEquals("New Anime Title", dbActual.title)
        assertEquals(9999, dbActual.id)
    }

    @Test
    fun testUpsertCreate() {
        val upsertData = AnimeInfo(
            id = 8888,
            title = "Cyberpunk: Edgerunners",
            link = "https://example.com/anime/8888",
            image = "https://example.com/img/8888.jpg",
            score = 8.6,
            members = 500000,
            genre = "Sci-Fi, Action",
            studios = "Trigger",
            source = "Video Game",
            season = "FALL",
            year = 2022,
            rank = 80,
            popularity = 120,
            scoringCount = 450000,
            episodes = 10,
            airStatus = "Finished Airing",
            type = "ONA",
            startDate = LocalDateTime.of(2022, 9, 13, 0, 0, 0),
            endDate = LocalDateTime.of(2022, 9, 13, 0, 0, 0),
            englishTitle = "Cyberpunk: Edgerunners",
            japaneseTitle = "サイバーパン크 エッジランナーズ",
            synopsis = "A street kid trying to survive in a technology and body modification-obsessed city of the future.",
            largeImage = "https://example.com/img/8888_large.jpg",
            rating = "R - 17+",
            nsfw = "SFW"
        )

        val requestBody = mapper.writeValueAsString(upsertData)

        val response = template.post("http://localhost:8081/anime/upsert", "application/json", requestBody)
        val actual: AnimeInfo = mapper.readValue(response, object : TypeReference<AnimeInfo>() {})

        assertEquals(8888, actual.id)
        assertEquals("Cyberpunk: Edgerunners", actual.title)
        assertEquals("anime upserted", actual.info)
        assertEquals(100, actual.episodeDistribution?.get(1)?.get("count"))

        val idParam = Pair("id", "8888")
        val getResponse = template.get("http://localhost:8081/anime", "application/json", idParam)
        val dbActual: AnimeInfo = mapper.readValue(getResponse, object : TypeReference<AnimeInfo>() {})

        assertEquals("Cyberpunk: Edgerunners", dbActual.title)
        assertEquals(8888, dbActual.id)
    }

    @Test
    fun testUpsertUpdate() {
        TestScenarioSupport(dataSource).loadTestScenario("jacks-test-scenario")

        val upsertData = AnimeInfo(
            id = 4765,
            title = "Attack on Titan - Upsert Updated",
            link = "https://example.com/anime/4765-upsert",
            image = "https://example.com/img/4765.jpg",
            score = 9.88,
            members = 3000000,
            genre = "Action, Fantasy",
            studios = "WIT Studio",
            source = "Manga",
            season = "SPRING",
            year = 2013,
            rank = 1,
            popularity = 1,
            scoringCount = 2000000,
            episodes = 25,
            airStatus = "Finished Airing",
            type = "TV",
            startDate = LocalDateTime.of(2013, 4, 7, 0, 0, 0),
            endDate = LocalDateTime.of(2013, 9, 29, 0, 0, 0),
            englishTitle = "Attack on Titan",
            japaneseTitle = "Shingeki no Kyojin",
            synopsis = "Centuries ago, mankind was slaughtered to near extinction...",
            largeImage = "https://example.com/img/4765_large.jpg",
            rating = "R - 17+",
            nsfw = "SFW"
        )

        val requestBody = mapper.writeValueAsString(upsertData)

        val response = template.post("http://localhost:8081/anime/upsert", "application/json", requestBody)
        val actual: AnimeInfo = mapper.readValue(response, object : TypeReference<AnimeInfo>() {})

        assertEquals(4765, actual.id)
        assertEquals("Attack on Titan - Upsert Updated", actual.title)
        assertEquals(9.88, actual.score)
        assertEquals(1, actual.rank)
        assertEquals("anime upserted", actual.info)
        assertEquals(100, actual.episodeDistribution?.get(1)?.get("count"))

        val idParam = Pair("id", "4765")
        val getResponse = template.get("http://localhost:8081/anime", "application/json", idParam)
        val dbActual: AnimeInfo = mapper.readValue(getResponse, object : TypeReference<AnimeInfo>() {})

        assertEquals("Attack on Titan - Upsert Updated", dbActual.title)
        assertEquals(9.88, dbActual.score)
    }
}