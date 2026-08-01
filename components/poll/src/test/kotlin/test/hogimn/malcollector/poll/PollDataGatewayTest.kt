package test.hogimn.malcollector.poll

import com.hogimn.malcollector.jdbcsupport.DataSourceConfig
import com.hogimn.malcollector.jdbcsupport.JdbcTemplate
import com.hogimn.malcollector.poll.PollDataGateway
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PollDataGatewayTest {
    private val dataSource = DataSourceConfig().createDataSource(
        "jdbc:mysql://localhost:3306/test_poll?user=uservices&password=uservices"
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
        val contentType = "anime"
        val topicId = 101
        val pollOptionId = 1

        val createdRecord = gateway.create(
            contentId = contentId,
            contentType = contentType,
            topicId = topicId,
            pollOptionId = pollOptionId,
            title = "To You, in 2000 Years: The Fall of Shiganshina, Part 1",
            episode = 1,
            votes = 15240,
        )

        assertEquals(contentId, createdRecord.contentId)
        assertEquals(contentType, createdRecord.contentType)
        assertEquals(topicId, createdRecord.topicId)
        assertEquals(pollOptionId, createdRecord.pollOptionId)
        assertEquals("To You, in 2000 Years: The Fall of Shiganshina, Part 1", createdRecord.title)

        val actual = template.query(
            "select content_id, content_type, topic_id, poll_option_id, title, episode, votes from poll where content_id = ? and content_type = ? and topic_id = ? and poll_option_id = ?",
            { ps ->
                ps.setInt(1, contentId)
                ps.setString(2, contentType)
                ps.setInt(3, topicId)
                ps.setInt(4, pollOptionId)
            },
            { rs ->
                listOf(
                    rs.getInt("content_id"),
                    rs.getString("content_type"),
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
        assertEquals(contentType, actual[1])
        assertEquals(topicId, actual[2])
        assertEquals(pollOptionId, actual[3])
        assertEquals("To You, in 2000 Years: The Fall of Shiganshina, Part 1", actual[4])
        assertEquals(1, actual[5])
        assertEquals(15240, actual[6])
    }

    @Test
    fun testFindBy() {
        val contentId = 7777
        val contentType = "manga"
        val topicId = 202
        val pollOptionId = 3

        val insertSql = """
            insert into poll (content_id, content_type, topic_id, poll_option_id, title, episode, votes, created_at, updated_at)
            values ($contentId, '$contentType', $topicId, $pollOptionId, 'Test Poll Title', 5, 99, NOW(), NOW())
        """.trimIndent()

        template.execute(insertSql)

        val result = gateway.findObject(contentId, contentType, topicId, pollOptionId)

        assertNotNull(result)
        assertEquals(contentId, result.contentId)
        assertEquals(contentType, result.contentType)
        assertEquals(topicId, result.topicId)
        assertEquals(pollOptionId, result.pollOptionId)
        assertEquals("Test Poll Title", result.title)
        assertEquals(5, result.episode)
        assertEquals(99, result.votes)
    }

    @Test
    fun testFindByContent() {
        val contentId = 7777
        val contentType = "manga"
        val topicId = 202
        val pollOptionId = 3

        val insertSql = """
            insert into poll (content_id, content_type, topic_id, poll_option_id, title, episode, votes, created_at, updated_at)
            values ($contentId, '$contentType', $topicId, $pollOptionId, 'Test Poll Title', 5, 99, NOW(), NOW())
        """.trimIndent()

        template.execute(insertSql)

        val result = gateway.findByContentId(contentId, contentType)
        val firstResult = result.first()

        assertEquals(1, result.size)
        assertEquals(contentId, firstResult.contentId)
        assertEquals(contentType, firstResult.contentType)
        assertEquals(topicId, firstResult.topicId)
        assertEquals(pollOptionId, firstResult.pollOptionId)
        assertEquals("Test Poll Title", firstResult.title)
        assertEquals(5, firstResult.episode)
        assertEquals(99, firstResult.votes)
    }

    @Test
    fun testUpdate() {
        val contentId = 4765
        val contentType = "anime"
        val topicId = 101
        val pollOptionId = 1

        gateway.create(
            contentId = contentId,
            contentType = contentType,
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
            contentType = contentType,
            topicId = topicId,
            pollOptionId = pollOptionId,
            title = updatedTitle,
            episode = updatedEpisode,
            votes = updatedVotes
        )

        assertEquals(1, updatedCount)

        val updatedRecord = gateway.findObject(contentId, contentType, topicId, pollOptionId)

        assertNotNull(updatedRecord)
        assertEquals(contentType, updatedRecord.contentType)
        assertEquals(updatedTitle, updatedRecord.title)
        assertEquals(updatedEpisode, updatedRecord.episode)
        assertEquals(updatedVotes, updatedRecord.votes)

        assert(
            updatedRecord.updatedAt.isAfter(updatedRecord.createdAt)
                    || updatedRecord.updatedAt.isEqual(updatedRecord.createdAt)
        )
    }

    @Test
    fun testUpsert() {
        val contentId = 1234
        val contentType = "lightnovel"
        val topicId = 555
        val pollOptionId = 9

        val insertedRecord = gateway.upsert(
            contentId = contentId,
            contentType = contentType,
            topicId = topicId,
            pollOptionId = pollOptionId,
            title = "First Initial Title",
            episode = 1,
            votes = 50
        )

        assertEquals("First Initial Title", insertedRecord.title)
        assertEquals(50, insertedRecord.votes)

        val dbRecordAfterInsert = gateway.findObject(contentId, contentType, topicId, pollOptionId)
        assertNotNull(dbRecordAfterInsert)
        assertEquals(contentType, dbRecordAfterInsert.contentType)
        assertEquals("First Initial Title", dbRecordAfterInsert.title)
        assertEquals(50, dbRecordAfterInsert.votes)

        val updatedRecord = gateway.upsert(
            contentId = contentId,
            contentType = contentType,
            topicId = topicId,
            pollOptionId = pollOptionId,
            title = "Upserted/Updated Title",
            episode = 2,
            votes = 150
        )

        assertEquals("Upserted/Updated Title", updatedRecord.title)
        assertEquals(2, updatedRecord.episode)
        assertEquals(150, updatedRecord.votes)

        val dbRecordAfterUpsert = gateway.findObject(contentId, contentType, topicId, pollOptionId)
        assertNotNull(dbRecordAfterUpsert)
        assertEquals(contentType, dbRecordAfterUpsert.contentType)
        assertEquals("Upserted/Updated Title", dbRecordAfterUpsert.title)
        assertEquals(2, dbRecordAfterUpsert.episode)
        assertEquals(150, dbRecordAfterUpsert.votes)
    }

    @Test
    fun testFindDistinctContentIds() {
        val targetType = "anime"
        val otherType = "manga"

        gateway.create(
            contentId = 101,
            contentType = targetType,
            topicId = 1,
            pollOptionId = 1,
            title = "A",
            episode = 1,
            votes = 10
        )
        gateway.create(
            contentId = 101,
            contentType = targetType,
            topicId = 1,
            pollOptionId = 2,
            title = "B",
            episode = 1,
            votes = 20
        )
        gateway.create(
            contentId = 102,
            contentType = targetType,
            topicId = 2,
            pollOptionId = 1,
            title = "C",
            episode = 2,
            votes = 30
        )
        gateway.create(
            contentId = 201,
            contentType = otherType,
            topicId = 3,
            pollOptionId = 1,
            title = "D",
            episode = 1,
            votes = 40
        )

        val distinctIds = gateway.findDistinctContentIds(targetType)

        assertEquals(2, distinctIds.size)
        assert(distinctIds.contains(101))
        assert(distinctIds.contains(102))
        assert(!distinctIds.contains(201))
    }

    @Test
    fun testFindByContentIds() {
        val contentType = "anime"
        val contentId1 = 1001
        val contentId2 = 1002

        gateway.create(
            contentId = contentId1,
            contentType = contentType,
            topicId = 101,
            pollOptionId = 1,
            title = "Title 1",
            episode = 1,
            votes = 100
        )
        gateway.create(
            contentId = contentId2,
            contentType = contentType,
            topicId = 102,
            pollOptionId = 1,
            title = "Title 2",
            episode = 1,
            votes = 200
        )

        val results = gateway.findByContentIds(listOf(contentId1, contentId2), contentType)
        assertEquals(2, results.size)
        val foundIds = results.map { it.contentId }.distinct()
        assert(foundIds.contains(contentId1))
        assert(foundIds.contains(contentId2))

        val emptyResults = gateway.findByContentIds(emptyList(), contentType)
        assertEquals(0, emptyResults.size)
    }
}