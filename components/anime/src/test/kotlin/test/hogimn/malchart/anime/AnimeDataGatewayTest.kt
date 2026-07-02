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
            largeImage = "large_img.jpg", rating = "PG-13", nsfw = "SAFE"
        )

        assertEquals(testId, createdRecord.id)
        assertEquals("For the Sake of Sita", createdRecord.title)

        val actual = gateway.findObject(testId)

        assertNotNull(actual)
        assertEquals(testId, actual.id)
        assertEquals("For the Sake of Sita", actual.title)
        assertEquals(9.5, actual.score)
        assertEquals("SUMMER", actual.season)
        assertEquals(2026, actual.year)
        assertEquals("A mystical fantasy romance story.", actual.synopsis)
    }

    @Test
    fun testFindBy() {
        val testId = 7777
        val nowTime = LocalDateTime.now()

        gateway.create(
            id = testId, title = "Test Anime", link = "#", image = "#",
            score = 8.8, members = 500, genre = "Sci-Fi", studios = "Trigger",
            source = "Original", season = "WINTER", year = 2026, rank = 10, popularity = 50,
            scoringCount = 450, episodes = 24, airStatus = "AIRING", type = "TV",
            startDate = nowTime, endDate = nowTime, englishTitle = "Test Anime",
            japaneseTitle = "テスト", synopsis = "Synopsis text",
            largeImage = "#", rating = "R", nsfw = "SAFE"
        )

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
        val nowTime = LocalDateTime.now()

        gateway.create(
            id = testId, title = "Original Title", link = "https://link.com", image = "img.jpg",
            score = 7.0, members = 1000, genre = "Action", studios = "A-1 Pictures",
            source = "Manga", season = "SPRING", year = 2026, rank = 50, popularity = 500,
            scoringCount = 800, episodes = 12, airStatus = "AIRING", type = "TV",
            startDate = nowTime, endDate = nowTime, englishTitle = "Original Title",
            japaneseTitle = "オリジナル", synopsis = "Original synopsis.",
            largeImage = "large_img.jpg", rating = "PG-13", nsfw = "SAFE"
        )

        val updatedTitle = "Updated Title"
        val updatedScore = 9.0
        val updatedSynopsis = "This is an updated synopsis."

        val updatedRows = gateway.update(
            id = testId, title = updatedTitle, link = "https://link.com", image = "img.jpg",
            score = updatedScore, members = 1000, genre = "Action", studios = "A-1 Pictures",
            source = "Manga", season = "SPRING", year = 2026, rank = 50, popularity = 500,
            scoringCount = 800, episodes = 12, airStatus = "AIRING", type = "TV",
            startDate = nowTime, endDate = nowTime, englishTitle = "Original Title",
            japaneseTitle = "オリジナル", synopsis = updatedSynopsis,
            largeImage = "large_img.jpg", rating = "PG-13", nsfw = "SAFE"
        )

        assertEquals(1, updatedRows)

        val updatedRecord = gateway.findObject(testId)

        assertNotNull(updatedRecord)
        assertEquals(updatedTitle, updatedRecord.title)
        assertEquals(updatedScore, updatedRecord.score)
        assertEquals(updatedSynopsis, updatedRecord.synopsis)
        assertEquals("SPRING", updatedRecord.season)
        assertEquals(2026, updatedRecord.year)
        assert(
            updatedRecord.updatedAt.isAfter(updatedRecord.createdAt)
                    || updatedRecord.updatedAt.isEqual(updatedRecord.createdAt)
        )
    }
}