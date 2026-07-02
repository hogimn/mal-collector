package com.hogimn.malchart.poll

import com.hogimn.malchart.jdbcsupport.JdbcTemplate
import java.time.LocalDateTime

class PollDataGateway(val jdbcTemplate: JdbcTemplate) {
    val selectSql = """
        select content_id, topic_id, poll_option_id, title, episode, votes, created_at, updated_at
        from poll
        where content_id = ? and poll_option_id = ? and topic_id = ?
    """.trimIndent()

    val createSql = """
        insert into poll (content_id, topic_id, poll_option_id, title, episode, votes, created_at, updated_at) 
        values (?, ?, ?, ?, ?, ?, ?, ?)
    """.trimIndent()

    val updateSql = """
        update poll set
            title = ?, episode = ?, votes = ?, updated_at = ?
        where content_id = ? and topic_id = ? and poll_option_id = ?
    """.trimIndent()

    fun create(
        contentId: Int,
        topicId: Int,
        pollOptionId: Int,
        title: String,
        episode: Int,
        votes: Int,
    ): PollRecord {
        val now = LocalDateTime.now()

        return jdbcTemplate.create(
            createSql,
            { PollRecord(contentId, topicId, pollOptionId, title, episode, votes, now, now) },
            contentId, topicId, pollOptionId, title, episode, votes, now, now
        )
    }

    fun findObject(contentId: Int, topicId: Int, pollOptionId: Int): PollRecord? {
        return jdbcTemplate.findObject(
            selectSql,
            { rs ->
                PollRecord(
                    contentId = rs.getInt("content_id"),
                    topicId = rs.getInt("topic_id"),
                    pollOptionId = rs.getInt("poll_option_id"),
                    title = rs.getString("title"),
                    episode = rs.getInt("episode"),
                    votes = rs.getInt("votes"),
                    createdAt = rs.getTimestamp("created_at").toLocalDateTime(),
                    updatedAt = rs.getTimestamp("updated_at").toLocalDateTime()
                )
            },
            contentId, pollOptionId, topicId
        )
    }

    fun update(
        contentId: Int,
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
            contentId, topicId, pollOptionId
        )
    }
}