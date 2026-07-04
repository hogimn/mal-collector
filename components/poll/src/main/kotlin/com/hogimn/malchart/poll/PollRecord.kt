package com.hogimn.malchart.poll

import java.time.LocalDateTime

data class PollRecord(
    val contentId: Int,
    val contentType: String,
    val topicId: Int,
    val pollOptionId: Int,
    val title: String,
    val episode: Int,
    val votes: Int,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)