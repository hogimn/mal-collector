package com.hogimn.malchart.mal

import java.time.LocalDateTime

data class AnimeInfo(
    val id: Int,
    val title: String,
    val link: String,
    val image: String,
    val score: Double,
    val members: Int,
    val genre: String,
    val studios: String,
    val source: String,
    val season: String,
    val year: Int,
    val rank: Int,
    val popularity: Int,
    val scoringCount: Int,
    val episodes: Int,
    val airStatus: String,
    val type: String,
    val startDate: LocalDateTime,
    val endDate: LocalDateTime,
    val englishTitle: String,
    val japaneseTitle: String,
    val synopsis: String,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
    val largeImage: String,
    val rating: String,
    val nsfw: String,
    val info: String? = null
)
