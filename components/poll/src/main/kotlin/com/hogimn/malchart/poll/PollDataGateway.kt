package com.hogimn.malchart.poll

import com.hogimn.malchart.jdbcsupport.JdbcTemplate
import java.sql.ResultSet
import java.time.LocalDateTime

class PollDataGateway(val jdbcTemplate: JdbcTemplate) {
    val selectSql = """
        select content_id, content_type, topic_id, poll_option_id, title, episode, votes, created_at, updated_at
        from poll
        where content_id = ? and content_type = ? and poll_option_id = ? and topic_id = ?
    """.trimIndent()

    val selectListSql = """
        select content_id, content_type, topic_id, poll_option_id, title, episode, votes, created_at, updated_at
        from poll
        where content_id = ? and content_type = ?
        order by episode, poll_option_id
    """.trimIndent()

    val createSql = """
        insert into poll (content_id, content_type, topic_id, poll_option_id, title, episode, votes, created_at, updated_at) 
        values (?, ?, ?, ?, ?, ?, ?, ?, ?)
    """.trimIndent()

    val updateSql = """
        update poll set
            title = ?, episode = ?, votes = ?, updated_at = ?
        where content_id = ? and content_type = ? and topic_id = ? and poll_option_id = ?
    """.trimIndent()

    val upsertSql = """
        insert into poll (content_id, content_type, topic_id, poll_option_id, title, episode, votes, created_at, updated_at) 
        values (?, ?, ?, ?, ?, ?, ?, ?, ?)
        on duplicate key update
            title = values(title),
            episode = values(episode),
            votes = values(votes),
            updated_at = values(updated_at)
    """.trimIndent()

    fun create(
        contentId: Int,
        contentType: String,
        topicId: Int,
        pollOptionId: Int,
        title: String,
        episode: Int,
        votes: Int,
    ): PollRecord {
        val now = LocalDateTime.now()

        return jdbcTemplate.create(
            createSql,
            {
                PollRecord(
                    contentId = contentId,
                    contentType = contentType,
                    topicId = topicId,
                    pollOptionId = pollOptionId,
                    title = title,
                    episode = episode,
                    votes = votes,
                    createdAt = now,
                    updatedAt = now
                )
            },
            contentId, contentType, topicId, pollOptionId, title, episode, votes, now, now
        )
    }

    fun findObject(contentId: Int, contentType: String, topicId: Int, pollOptionId: Int): PollRecord? {
        return jdbcTemplate.findObject(
            selectSql,
            ::mapRow,
            contentId, contentType, pollOptionId, topicId
        )
    }

    fun findByContentId(contentId: Int, contentType: String): List<PollRecord> {
        return jdbcTemplate.findList(
            selectListSql,
            ::mapRow,
            contentId, contentType
        )
    }

    fun update(
        contentId: Int,
        contentType: String,
        topicId: Int,
        pollOptionId: Int,
        title: String,
        episode: Int,
        votes: Int
    ): Int {
        val now = LocalDateTime.now()
        return jdbcTemplate.update(
            updateSql,
            title, episode, votes, now,
            contentId, contentType, topicId, pollOptionId
        )
    }

    fun upsert(
        contentId: Int,
        contentType: String,
        topicId: Int,
        pollOptionId: Int,
        title: String,
        episode: Int,
        votes: Int,
    ): PollRecord {
        val now = LocalDateTime.now()

        return jdbcTemplate.create(
            upsertSql,
            {
                PollRecord(
                    contentId = contentId,
                    contentType = contentType,
                    topicId = topicId,
                    pollOptionId = pollOptionId,
                    title = title,
                    episode = episode,
                    votes = votes,
                    createdAt = now,
                    updatedAt = now
                )
            },
            contentId, contentType, topicId, pollOptionId, title, episode, votes, now, now
        )
    }

    private fun mapRow(rs: ResultSet): PollRecord {
        return PollRecord(
            contentId = rs.getInt("content_id"),
            contentType = rs.getString("content_type"),
            topicId = rs.getInt("topic_id"),
            pollOptionId = rs.getInt("poll_option_id"),
            title = rs.getString("title"),
            episode = rs.getInt("episode"),
            votes = rs.getInt("votes"),
            createdAt = rs.getTimestamp("created_at").toLocalDateTime(),
            updatedAt = rs.getTimestamp("updated_at").toLocalDateTime()
        )
    }
}