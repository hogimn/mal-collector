package test.hogimn.malcollector.poll

import com.fasterxml.jackson.core.type.TypeReference
import com.hogimn.malcollector.discoverysupport.DiscoveryClient
import com.hogimn.malcollector.jdbcsupport.DataSourceConfig
import com.hogimn.malcollector.jdbcsupport.JdbcTemplate
import com.hogimn.malcollector.poll.*
import com.hogimn.malcollector.restsupport.BasicServer
import com.hogimn.malcollector.restsupport.RestTemplate
import com.hogimn.malcollector.testsupport.TestControllerSupport
import com.hogimn.malcollector.testsupport.TestScenarioSupport
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PollControllerTest : TestControllerSupport() {
    val dataSource =
        DataSourceConfig().createDataSource("jdbc:mysql://localhost:3306/test_poll?user=uservices&password=uservices")
    val restTemplate = RestTemplate()

    private val server = object : BasicServer(8081) {
        override fun registerContexts() {
            val dummyAnimeClient = object : AnimeClient(mapper, restTemplate, DiscoveryClient(mapper, restTemplate)) {
                override fun findIdsByYearAndSeason(year: Int, season: String): List<Int> {
                    return listOf(4765)
                }
            }

            context(
                "/poll",
                PollController(
                    mapper,
                    PollService(
                        dummyAnimeClient,
                        PollDataGateway(JdbcTemplate(dataSource))
                    )
                )
            )
        }
    }

    @Before
    fun setUp() {
        JdbcTemplate(dataSource).apply {
            execute("delete from poll")
        }
        server.start()
    }

    @After
    fun tearDown() {
        server.stop()
    }

    @Test
    fun testFind() {
        TestScenarioSupport(dataSource).loadTestScenario("jacks-test-scenario")

        val response = template.get(
            "http://localhost:8081/poll",
            "application/json",
            Pair("contentId", "4765"),
            Pair("contentType", "anime"),
            Pair("topicId", "101"),
            Pair("pollOptionId", "1")
        )
        val actual: PollInfo = mapper.readValue(response, object : TypeReference<PollInfo>() {})

        assertEquals(4765, actual.contentId)
        assertEquals("anime", actual.contentType)
        assertEquals(101, actual.topicId)
        assertEquals(1, actual.pollOptionId)
        assertEquals("To You, in 2000 Years: The Fall of Shiganshina, Part 1", actual.title)
        assertEquals(1, actual.episode)
        assertEquals(15240, actual.votes)
        assertEquals(LocalDateTime.of(2026, 6, 28, 21, 0, 0), actual.createdAt)
        assertEquals(LocalDateTime.of(2026, 6, 28, 21, 0, 0), actual.updatedAt)
        assertEquals("poll info", actual.info)
    }

    @Test
    fun testFindByContentId() {
        TestScenarioSupport(dataSource).loadTestScenario("jacks-test-scenario")

        val response = template.get(
            "http://localhost:8081/poll",
            "application/json",
            Pair("contentId", "4765"),
            Pair("contentType", "anime"),
        )
        val actual: List<PollInfo> = mapper.readValue(response, object : TypeReference<List<PollInfo>>() {})
        val actualFirst = actual.first()

        assertEquals(4765, actualFirst.contentId)
        assertEquals("anime", actualFirst.contentType)
        assertEquals(101, actualFirst.topicId)
        assertEquals(1, actualFirst.pollOptionId)
        assertEquals("To You, in 2000 Years: The Fall of Shiganshina, Part 1", actualFirst.title)
        assertEquals(1, actualFirst.episode)
        assertEquals(15240, actualFirst.votes)
        assertEquals(LocalDateTime.of(2026, 6, 28, 21, 0, 0), actualFirst.createdAt)
        assertEquals(LocalDateTime.of(2026, 6, 28, 21, 0, 0), actualFirst.updatedAt)
        assertEquals("poll info", actualFirst.info)
    }

    @Test
    fun testCreate() {
        val newPoll = PollInfo(
            contentId = 5555,
            contentType = "manga",
            topicId = 202,
            pollOptionId = 2,
            title = "A Sound Argument",
            episode = 5,
            votes = 8900
        )
        val requestBody = mapper.writeValueAsString(newPoll)

        val response = template.post(
            "http://localhost:8081/poll",
            "application/json",
            requestBody
        )

        val actual: PollInfo = mapper.readValue(response, object : TypeReference<PollInfo>() {})
        assertEquals(5555, actual.contentId)
        assertEquals("manga", actual.contentType)
        assertEquals(202, actual.topicId)
        assertEquals(2, actual.pollOptionId)
        assertEquals("A Sound Argument", actual.title)
        assertEquals(5, actual.episode)
        assertEquals(8900, actual.votes)
        assertEquals("poll created", actual.info)
        assertNotNull(actual.createdAt)
        assertNotNull(actual.updatedAt)
    }

    @Test
    fun testUpdate() {
        TestScenarioSupport(dataSource).loadTestScenario("jacks-test-scenario")

        val updatePoll = PollInfo(
            contentId = 4765,
            contentType = "anime",
            topicId = 101,
            pollOptionId = 1,
            title = "Updated Title: The Fall of Shiganshina, Part 1",
            episode = 2,
            votes = 16000,
        )
        val requestBody = mapper.writeValueAsString(updatePoll)

        val response = template.put(
            "http://localhost:8081/poll",
            "application/json",
            requestBody
        )

        val actual: PollInfo = mapper.readValue(response, object : TypeReference<PollInfo>() {})
        assertEquals(4765, actual.contentId)
        assertEquals("anime", actual.contentType)
        assertEquals("Updated Title: The Fall of Shiganshina, Part 1", actual.title)
        assertEquals(2, actual.episode)
        assertEquals(16000, actual.votes)
        assertEquals("poll updated", actual.info)

        val originalTime = LocalDateTime.of(2026, 6, 28, 21, 0, 0)
        assert(actual.updatedAt!!.isAfter(originalTime))
    }

    @Test
    fun testUpsertCreate() {
        val upsertPoll = PollInfo(
            contentId = 6666,
            contentType = "lightnovel",
            topicId = 303,
            pollOptionId = 3,
            title = "Brave New World",
            episode = 10,
            votes = 5000
        )
        val requestBody = mapper.writeValueAsString(upsertPoll)

        val response = template.post(
            "http://localhost:8081/poll/upsert",
            "application/json",
            requestBody
        )

        val actual: PollInfo = mapper.readValue(response, object : TypeReference<PollInfo>() {})
        assertEquals(6666, actual.contentId)
        assertEquals("lightnovel", actual.contentType)
        assertEquals(303, actual.topicId)
        assertEquals(3, actual.pollOptionId)
        assertEquals("Brave New World", actual.title)
        assertEquals(10, actual.episode)
        assertEquals(5000, actual.votes)
        assertEquals("poll upserted", actual.info)
        assertNotNull(actual.createdAt)
        assertNotNull(actual.updatedAt)

        val getResponse = template.get(
            "http://localhost:8081/poll",
            "application/json",
            Pair("contentId", "6666"),
            Pair("contentType", "lightnovel"),
            Pair("topicId", "303"),
            Pair("pollOptionId", "3")
        )
        val dbActual: PollInfo = mapper.readValue(getResponse, object : TypeReference<PollInfo>() {})
        assertEquals("Brave New World", dbActual.title)
        assertEquals(5000, dbActual.votes)
    }

    @Test
    fun testUpsertUpdate() {
        TestScenarioSupport(dataSource).loadTestScenario("jacks-test-scenario")

        val upsertPoll = PollInfo(
            contentId = 4765,
            contentType = "anime",
            topicId = 101,
            pollOptionId = 1,
            title = "Upsert Updated Title: The Fall of Shiganshina, Part 1",
            episode = 1,
            votes = 20000,
        )
        val requestBody = mapper.writeValueAsString(upsertPoll)

        val response = template.post(
            "http://localhost:8081/poll/upsert",
            "application/json",
            requestBody
        )

        val actual: PollInfo = mapper.readValue(response, object : TypeReference<PollInfo>() {})
        assertEquals(4765, actual.contentId)
        assertEquals("anime", actual.contentType)
        assertEquals("Upsert Updated Title: The Fall of Shiganshina, Part 1", actual.title)
        assertEquals(20000, actual.votes)
        assertEquals("poll upserted", actual.info)

        val getResponse = template.get(
            "http://localhost:8081/poll",
            "application/json",
            Pair("contentId", "4765"),
            Pair("contentType", "anime"),
            Pair("topicId", "101"),
            Pair("pollOptionId", "1")
        )
        val dbActual: PollInfo = mapper.readValue(getResponse, object : TypeReference<PollInfo>() {})
        assertEquals("Upsert Updated Title: The Fall of Shiganshina, Part 1", dbActual.title)
        assertEquals(20000, dbActual.votes)
    }

    @Test
    fun testFindContentIds() {
        val gateway = PollDataGateway(JdbcTemplate(dataSource))
        val targetType = "anime"

        gateway.create(
            contentId = 4765,
            contentType = targetType,
            topicId = 101,
            pollOptionId = 1,
            title = "Title A",
            episode = 1,
            votes = 100
        )
        gateway.create(
            contentId = 4765,
            contentType = targetType,
            topicId = 101,
            pollOptionId = 2,
            title = "Title B",
            episode = 1,
            votes = 150
        )
        gateway.create(
            contentId = 4766,
            contentType = targetType,
            topicId = 102,
            pollOptionId = 1,
            title = "Title C",
            episode = 2,
            votes = 200
        )
        gateway.create(
            contentId = 9999,
            contentType = "manga",
            topicId = 201,
            pollOptionId = 1,
            title = "Manga Title",
            episode = 1,
            votes = 50
        )

        val response = template.get(
            "http://localhost:8081/poll/content-ids",
            "application/json",
            Pair("contentType", targetType)
        )

        val actualIds: List<Int> = mapper.readValue(response, object : TypeReference<List<Int>>() {})

        assertEquals(2, actualIds.size)
        assert(actualIds.contains(4765))
        assert(actualIds.contains(4766))
        assert(!actualIds.contains(9999))
    }

    @Test
    fun testFindSummary() {
        val gateway = PollDataGateway(JdbcTemplate(dataSource))
        val targetContentId = 8888
        val targetType = "anime"

        gateway.create(
            contentId = targetContentId,
            contentType = targetType,
            topicId = 1,
            pollOptionId = 1,
            title = "Title A",
            episode = 1,
            votes = 200
        )
        gateway.create(
            contentId = targetContentId,
            contentType = targetType,
            topicId = 1,
            pollOptionId = 2,
            title = "Title A",
            episode = 1,
            votes = 200
        )
        gateway.create(
            contentId = targetContentId,
            contentType = targetType,
            topicId = 1,
            pollOptionId = 3,
            title = "Title A",
            episode = 1,
            votes = 200
        )
        gateway.create(
            contentId = targetContentId,
            contentType = targetType,
            topicId = 1,
            pollOptionId = 4,
            title = "Title A",
            episode = 1,
            votes = 200
        )
        gateway.create(
            contentId = targetContentId,
            contentType = targetType,
            topicId = 1,
            pollOptionId = 5,
            title = "Title A",
            episode = 1,
            votes = 450
        )

        val response = template.get(
            "http://localhost:8081/poll/summary",
            "application/json",
            Pair("contentId", targetContentId.toString()),
            Pair("contentType", targetType)
        )

        val actual: PollSummaryInfo = mapper.readValue(response, object : TypeReference<PollSummaryInfo>() {})

        assertEquals(targetContentId, actual.contentId)
        assertEquals(targetType, actual.contentType)
        assertEquals(1, actual.episodeDistribution.size)

        val ep1 = actual.episodeDistribution[1]
        assertNotNull(ep1)
        assertEquals("3.40", ep1["averageScore"])
        assertEquals(1250, ep1["votes"])
    }

    @Test
    fun testFindSeasonSummary() {
        val gateway = PollDataGateway(JdbcTemplate(dataSource))
        val targetContentId = 4765
        val targetType = "anime"

        gateway.create(
            contentId = targetContentId,
            contentType = targetType,
            topicId = 1,
            pollOptionId = 1,
            title = "To You, in 2000 Years",
            episode = 1,
            votes = 500
        )

        val response = template.get(
            "http://localhost:8081/poll/season-summary",
            "application/json",
            Pair("contentType", targetType),
            Pair("year", "2026"),
            Pair("season", "summer")
        )

        val actualList: List<PollSummaryInfo> =
            mapper.readValue(response, object : TypeReference<List<PollSummaryInfo>>() {})

        assertNotNull(actualList)
        assertEquals(1, actualList.size)
        assertEquals(targetContentId, actualList.first().contentId)
        assertEquals(targetType, actualList.first().contentType)
    }
}