package test.hogimn.malchart.poll

import com.fasterxml.jackson.core.type.TypeReference
import com.hogimn.malchart.jdbcsupport.DataSourceConfig
import com.hogimn.malchart.jdbcsupport.JdbcTemplate
import com.hogimn.malchart.poll.PollController
import com.hogimn.malchart.poll.PollDataGateway
import com.hogimn.malchart.poll.PollInfo
import com.hogimn.malchart.restsupport.BasicServer
import com.hogimn.malchart.testsupport.TestControllerSupport
import com.hogimn.malchart.testsupport.TestScenarioSupport
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PollControllerTest : TestControllerSupport() {
    val dataSource =
        DataSourceConfig().createDataSource("jdbc:mysql://localhost:3306/poll_test?user=uservices&password=uservices")

    private val server = object : BasicServer(8081) {
        override fun registerContexts() {
            context(
                "/poll",
                PollController(
                    mapper,
                    PollDataGateway(JdbcTemplate(dataSource))
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
            Pair("topicId", "101"),
            Pair("pollOptionId", "1")
        )
        val actual: PollInfo = mapper.readValue(response, object : TypeReference<PollInfo>() {})

        assertEquals(4765, actual.contentId)
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
    fun testCreate() {
        val newPoll = PollInfo(
            contentId = 5555,
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
        assertEquals("Updated Title: The Fall of Shiganshina, Part 1", actual.title)
        assertEquals(2, actual.episode)
        assertEquals(16000, actual.votes)
        assertEquals("poll updated", actual.info)

        val originalTime = LocalDateTime.of(2026, 6, 28, 21, 0, 0)
        assert(actual.updatedAt!!.isAfter(originalTime))
    }
}