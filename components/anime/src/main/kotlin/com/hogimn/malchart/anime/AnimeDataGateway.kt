package com.hogimn.malchart.anime

import com.hogimn.malchart.jdbcsupport.JdbcTemplate
import java.sql.ResultSet
import java.time.LocalDateTime

class AnimeDataGateway(val jdbcTemplate: JdbcTemplate) {
    private val createSql = """
        insert into anime (
            id, title, link, image, score, members, genre, studios, source, season, year, 
            `rank`, popularity, scoring_count, episodes, air_status, type, start_date, end_date, 
            english_title, japanese_title, synopsis, created_at, updated_at, large_image, rating, nsfw
        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    """.trimIndent()

    private val updateSql = """
        update anime set
            title = ?, link = ?, image = ?, score = ?, members = ?, genre = ?, studios = ?, 
            source = ?, season = ?, year = ?, `rank` = ?, popularity = ?, scoring_count = ?, 
            episodes = ?, air_status = ?, type = ?, start_date = ?, end_date = ?, 
            english_title = ?, japanese_title = ?, synopsis = ?, created_at = ?, updated_at = ?, 
            large_image = ?, rating = ?, nsfw = ?
        where id = ?
    """.trimIndent()

    private val selectSql = """
        select 
            id, title, link, image, score, members, genre, studios, source, season, year, 
            `rank`, popularity, scoring_count, episodes, air_status, type, start_date, end_date, 
            english_title, japanese_title, synopsis, created_at, updated_at, large_image, rating, nsfw
        from anime
    """.trimIndent()

    fun create(
        id: Int, title: String, link: String, image: String, score: Double, members: Int,
        genre: String, studios: String, source: String, season: String, year: Int,
        rank: Int, popularity: Int, scoringCount: Int, episodes: Int, airStatus: String,
        type: String, startDate: LocalDateTime, endDate: LocalDateTime, englishTitle: String, japaneseTitle: String,
        synopsis: String, createdAt: LocalDateTime, updatedAt: LocalDateTime, largeImage: String,
        rating: String, nsfw: String
    ): AnimeRecord {
        return jdbcTemplate.create(
            createSql, {
                AnimeRecord(
                    id, title, link, image, score, members, genre, studios, source, season, year,
                    rank, popularity, scoringCount, episodes, airStatus, type, startDate, endDate,
                    englishTitle, japaneseTitle, synopsis, createdAt, updatedAt, largeImage, rating, nsfw
                )
            }, id, title, link, image, score, members, genre, studios, source, season, year,
            rank, popularity, scoringCount, episodes, airStatus, type, startDate, endDate,
            englishTitle, japaneseTitle, synopsis, createdAt, updatedAt, largeImage, rating, nsfw
        )
    }

    fun update(
        id: Int, title: String, link: String, image: String, score: Double, members: Int,
        genre: String, studios: String, source: String, season: String, year: Int,
        rank: Int, popularity: Int, scoringCount: Int, episodes: Int, airStatus: String,
        type: String, startDate: LocalDateTime, endDate: LocalDateTime, englishTitle: String, japaneseTitle: String,
        synopsis: String, createdAt: LocalDateTime, updatedAt: LocalDateTime, largeImage: String,
        rating: String, nsfw: String
    ): Int {
        return jdbcTemplate.update(
            updateSql,
            title, link, image, score, members, genre, studios, source, season, year,
            rank, popularity, scoringCount, episodes, airStatus, type, startDate, endDate,
            englishTitle, japaneseTitle, synopsis, createdAt, updatedAt, largeImage, rating, nsfw,
            id
        )
    }

    fun findObject(id: Int): AnimeRecord? {
        val sql = "$selectSql where id = ?"
        return jdbcTemplate.findObject(sql, { rs -> mapRow(rs) }, id)
    }

    fun findByYearAndSeason(year: Int, season: String): List<AnimeRecord> {
        val sql = "$selectSql where year = ? and season = ?"
        return jdbcTemplate.query(
            sql,
            { ps ->
                ps.setInt(1, year)
                ps.setString(2, season)
            },
            { rs -> mapRow(rs) })
    }

    private fun mapRow(rs: ResultSet): AnimeRecord {
        return AnimeRecord(
            id = rs.getInt("id"),
            title = rs.getString("title"),
            link = rs.getString("link"),
            image = rs.getString("image"),
            score = rs.getDouble("score"),
            members = rs.getInt("members"),
            genre = rs.getString("genre"),
            studios = rs.getString("studios"),
            source = rs.getString("source"),
            season = rs.getString("season"),
            year = rs.getInt("year"),
            rank = rs.getInt("rank"),
            popularity = rs.getInt("popularity"),
            scoringCount = rs.getInt("scoring_count"),
            episodes = rs.getInt("episodes"),
            airStatus = rs.getString("air_status"),
            type = rs.getString("type"),
            startDate = rs.getTimestamp("start_date").toLocalDateTime(),
            endDate = rs.getTimestamp("end_date").toLocalDateTime(),
            englishTitle = rs.getString("english_title"),
            japaneseTitle = rs.getString("japanese_title"),
            synopsis = rs.getString("synopsis"),
            createdAt = rs.getTimestamp("created_at").toLocalDateTime(),
            updatedAt = rs.getTimestamp("updated_at").toLocalDateTime(),
            largeImage = rs.getString("large_image"),
            rating = rs.getString("rating"),
            nsfw = rs.getString("nsfw")
        )
    }
}