package com.hogimn.malcollector.mal

import java.time.LocalDateTime

data class PollInfo(
    val contentId: Int,
    val contentType: String,
    val topicId: Int,
    val pollOptionId: Int,
    val title: String,
    val episode: Int,
    val votes: Int,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
    val info: String? = null
)