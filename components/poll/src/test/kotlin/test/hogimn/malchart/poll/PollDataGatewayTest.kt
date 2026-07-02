package test.hogimn.malchart.poll

import com.hogimn.malchart.jdbcsupport.DataSourceConfig
import com.hogimn.malchart.jdbcsupport.JdbcTemplate
import com.hogimn.malchart.poll.PollDataGateway
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PollDataGatewayTest {
    private val dataSource = DataSourceConfig().createDataSource(
        "jdbc:mysql://localhost:3306/poll_test?user=uservices&password=uservices"
    )
    private val template = JdbcTemplate(dataSource)
    private val gateway = PollDataGateway(template)

    @Before
    fun cleanDatabase() {
        template.execute("delete from poll")
    }

    @Test
    fun testCreate() {
        val contentId = 4765
        val topicId = 101
        val pollOptionId = 1

        val createdRecord = gateway.create(
            contentId = contentId,
            topicId = topicId,
            pollOptionId = pollOptionId,
            title = "To You, in 2000 Years: The Fall of Shiganshina, Part 1",
            episode = 1,
            votes = 15240,
        )

        assertEquals(contentId, createdRecord.contentId)
        assertEquals(topicId, createdRecord.topicId)
        assertEquals(pollOptionId, createdRecord.pollOptionId)
        assertEquals("To You, in 2000 Years: The Fall of Shiganshina, Part 1", createdRecord.title)

        val actual = template.query(
            "select content_id, topic_id, poll_option_id, title, episode, votes from poll where content_id = ? and topic_id = ? and poll_option_id = ?",
            { ps ->
                ps.setInt(1, contentId)
                ps.setInt(2, topicId)
                ps.setInt(3, pollOptionId)
            },
            { rs ->
                listOf(
                    rs.getInt("content_id"),
                    rs.getInt("topic_id"),
                    rs.getInt("poll_option_id"),
                    rs.getString("title"),
                    rs.getInt("episode"),
                    rs.getInt("votes")
                )
            }
        ).firstOrNull()

        assertNotNull(actual, "Data was not normally saved in the DB.")
        assertEquals(contentId, actual[0])
        assertEquals(topicId, actual[1])
        assertEquals(pollOptionId, actual[2])
        assertEquals("To You, in 2000 Years: The Fall of Shiganshina, Part 1", actual[3])
        assertEquals(1, actual[4])
        assertEquals(15240, actual[5])
    }

    @Test
    fun testFindBy() {
        val contentId = 7777
        val topicId = 202
        val pollOptionId = 3

        val insertSql = """
            insert into poll (content_id, topic_id, poll_option_id, title, episode, votes, created_at, updated_at)
            values ($contentId, $topicId, $pollOptionId, 'Test Poll Title', 5, 99, NOW(), NOW())
        """.trimIndent()

        template.execute(insertSql)

        val result = gateway.findObject(contentId, topicId, pollOptionId)

        assertNotNull(result)
        assertEquals(contentId, result.contentId)
        assertEquals(topicId, result.topicId)
        assertEquals(pollOptionId, result.pollOptionId)
        assertEquals("Test Poll Title", result.title)
        assertEquals(5, result.episode)
        assertEquals(99, result.votes)
    }

    @Test
    fun testUpdate() {
        val contentId = 4765
        val topicId = 101
        val pollOptionId = 1

        gateway.create(
            contentId = contentId,
            topicId = topicId,
            pollOptionId = pollOptionId,
            title = "Before Title",
            episode = 1,
            votes = 10,
        )

        val updatedTitle = "That's It, That's the Episode Title"
        val updatedEpisode = 2
        val updatedVotes = 500

        val updatedCount = gateway.update(
            contentId = contentId,
            topicId = topicId,
            pollOptionId = pollOptionId,
            title = updatedTitle,
            episode = updatedEpisode,
            votes = updatedVotes
        )

        assertEquals(1, updatedCount)

        val updatedRecord = gateway.findObject(contentId, topicId, pollOptionId)

        assertNotNull(updatedRecord)
        assertEquals(updatedTitle, updatedRecord.title)
        assertEquals(updatedEpisode, updatedRecord.episode)
        assertEquals(updatedVotes, updatedRecord.votes)

        assert(
            updatedRecord.updatedAt.isAfter(updatedRecord.createdAt)
                    || updatedRecord.updatedAt.isEqual(updatedRecord.createdAt)
        )
    }
}