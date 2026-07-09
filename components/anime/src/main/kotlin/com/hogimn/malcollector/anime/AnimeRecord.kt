package com.hogimn.malcollector.anime

import java.time.LocalDateTime

data class AnimeRecord(
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
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val largeImage: String,
    val rating: String,
    val nsfw: String
)