package test.hogimn.malchart.anime

import com.hogimn.malchart.anime.AnimeDataGateway
import com.hogimn.malchart.jdbcsupport.DataSourceConfig
import com.hogimn.malchart.jdbcsupport.JdbcTemplate
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AnimeDataGatewayTest {
    private val dataSource = DataSourceConfig().createDataSource(
        "jdbc:mysql://localhost:3306/anime_test?user=uservices&password=uservices"
    )
    private val template = JdbcTemplate(dataSource)
    private val gateway = AnimeDataGateway(template)

    @Before
    fun cleanDatabase() {
        template.execute("delete from anime")
    }

    @Test
    fun testCreate() {
        val testId = 9999
        val nowTime = LocalDateTime.now()

        val createdRecord = gateway.create(
            id = testId, title = "For the Sake of Sita", link = "https://link.com", image = "img.jpg",
            score = 9.5, members = 15000, genre = "Drama, Fantasy", studios = "LICO",
            source = "Webtoon", season = "SUMMER", year = 2026, rank = 1, popularity = 120,
            scoringCount = 9800, episodes = 12, airStatus = "FINISHED", type = "ONA",
            startDate = nowTime, endDate = nowTime, englishTitle = "For the Sake of Sita",
            japaneseTitle = "シタのために", synopsis = "A mystical fantasy romance story.",
            createdAt = nowTime, updatedAt = nowTime, largeImage = "large_img.jpg",
            rating = "PG-13", nsfw = "SAFE"
        )

        assertEquals(testId, createdRecord.id)
        assertEquals("For the Sake of Sita", createdRecord.title)

        val actual = template.query(
            "select id, title, score, season, year, synopsis from anime where id = ?",
            { ps -> ps.setInt(1, testId) },
            { rs ->
                listOf(
                    rs.getInt("id"),
                    rs.getString("title"),
                    rs.getDouble("score"),
                    rs.getString("season"),
                    rs.getInt("year"),
                    rs.getString("synopsis")
                )
            }
        ).firstOrNull()

        assertNotNull(actual, "Data was not normally saved in the DB.")
        assertEquals(testId, actual[0])
        assertEquals("For the Sake of Sita", actual[1])
        assertEquals(9.5, actual[2])
        assertEquals("SUMMER", actual[3])
        assertEquals(2026, actual[4])
        assertEquals("A mystical fantasy romance story.", actual[5])
    }

    @Test
    fun testFindBy() {
        val testId = 7777
        val insertSql = """
        insert into anime (
            id, title, link, image, score, members, genre, studios, source, season, year, 
            `rank`, popularity, scoring_count, episodes, air_status, type, start_date, end_date, 
            english_title, japanese_title, synopsis, created_at, updated_at, large_image, rating, nsfw
        ) values (${testId}, 'Test Anime', '#', '#', 8.8, 500, 'Sci-Fi', 'Trigger', 'Original', 'WINTER', 2026, 
                 10, 50, 450, 24, 'AIRING', 'TV', NOW(), NOW(), 'Test Anime', 'テスト', 'Synopsis text', NOW(), NOW(), '#', 'R', 'SAFE')
    """.trimIndent()

        template.execute(insertSql)

        val result = gateway.findObject(testId)

        assertNotNull(result)

        assertEquals(testId, result.id)
        assertEquals("Test Anime", result.title)
        assertEquals(8.8, result.score)
        assertEquals("WINTER", result.season)
        assertEquals(2026, result.year)
        assertEquals("Sci-Fi", result.genre)
    }

    @Test
    fun testUpdate() {
        val testId = 5555
        val nowTime = LocalDateTime.now().withNano(0)

        gateway.create(
            id = testId, title = "Original Title", link = "https://link.com", image = "img.jpg",
            score = 7.0, members = 1000, genre = "Action", studios = "A-1 Pictures",
            source = "Manga", season = "SPRING", year = 2026, rank = 50, popularity = 500,
            scoringCount = 800, episodes = 12, airStatus = "AIRING", type = "TV",
            startDate = nowTime, endDate = nowTime, englishTitle = "Original Title",
            japaneseTitle = "オリジナル", synopsis = "Original synopsis.",
            createdAt = nowTime, updatedAt = nowTime, largeImage = "large_img.jpg",
            rating = "PG-13", nsfw = "SAFE"
        )

        val updatedTime = nowTime.plusHours(1)
        val updatedRows = gateway.update(
            id = testId, title = "Updated Title", link = "https://link.com", image = "img.jpg",
            score = 9.0, members = 1000, genre = "Action", studios = "A-1 Pictures",
            source = "Manga", season = "SPRING", year = 2026, rank = 50, popularity = 500,
            scoringCount = 800, episodes = 12, airStatus = "AIRING", type = "TV",
            startDate = nowTime, endDate = nowTime, englishTitle = "Original Title",
            japaneseTitle = "オリジナル", synopsis = "This is an updated synopsis.",
            createdAt = nowTime, updatedAt = updatedTime, largeImage = "large_img.jpg",
            rating = "PG-13", nsfw = "SAFE"
        )

        assertEquals(1, updatedRows)

        val updatedRecord = gateway.findObject(testId)

        assertNotNull(updatedRecord)
        assertEquals("Updated Title", updatedRecord.title)
        assertEquals(9.0, updatedRecord.score)
        assertEquals("This is an updated synopsis.", updatedRecord.synopsis)
        assertEquals(updatedTime, updatedRecord.updatedAt)
        assertEquals("SPRING", updatedRecord.season)
        assertEquals(2026, updatedRecord.year)
    }
}