package test.hogimn.malchart

import com.hogimn.malchart.jdbcsupport.DataSourceConfig
import com.hogimn.malchart.jdbcsupport.JdbcTemplate
import com.hogimn.malchart.redissupport.RedisConfig
import com.hogimn.malchart.restsupport.RestTemplate
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals

class FlowTest {
    val template = RestTemplate()

    lateinit var discovery: Process
    lateinit var anime: Process
    lateinit var poll: Process

    @Before
    fun setUp() {
        val userDir = System.getProperty("user.dir")

        RedisConfig().getClient("localhost", "foobared").flushAll()

        JdbcTemplate(
            DataSourceConfig()
                .createDataSource("jdbc:mysql://localhost:3306/test_anime?user=uservices&password=uservices")
        ).apply {
            execute("delete from anime")
        }
        JdbcTemplate(
            DataSourceConfig()
                .createDataSource("jdbc:mysql://localhost:3306/test_poll?user=uservices&password=uservices")
        ).apply {
            execute("delete from poll")
        }

        discovery = runCommand(
            8890,
            "discovery",
            "java -jar $userDir/../discovery-server/build/libs/discovery-server.jar",
            File(userDir)
        )

        anime = runCommand(
            8891,
            "anime",
            "java -jar $userDir/../anime-server/build/libs/anime-server.jar",
            File(userDir)
        )
        poll = runCommand(
            8892,
            "poll",
            "java -jar $userDir/../poll-server/build/libs/poll-server.jar",
            File(userDir)
        )
    }

    @After
    fun tearDown() {
        discovery.destroy()
        anime.destroy()
        poll.destroy()
    }

    @Test
    fun testBasicFlow() {
        listOf(8890, 8891, 8892).forEach { waitUntilReady("http://localhost:$it") }

        var response: String?

        val discoveryServer = "http://localhost:8890"
        response = template.get(discoveryServer, "application/json")
        assertEquals("Noop!", response)

        val animeServer = "http://localhost:8891"

        response = template.get(animeServer, "application/json")
        assertEquals("Noop!", response)

        val animeId = "9999"
        val newAnimeJson = """
            {
              "id": $animeId,
              "title": "New E2E Test Anime",
              "link": "https://example.com/anime/$animeId",
              "image": "https://example.com/img/$animeId.jpg",
              "score": 8.8,
              "members": 120000,
              "genre": "Sci-Fi, Action",
              "studios": "Trigger",
              "source": "Original",
              "season": "SUMMER",
              "year": 2026,
              "rank": 100,
              "popularity": 250,
              "scoringCount": 95000,
              "episodes": 12,
              "airStatus": "Currently Airing",
              "type": "TV",
              "startDate": "2026-07-01T00:00:00",
              "endDate": "2026-09-23T00:00:00",
              "englishTitle": "New Anime English",
              "japaneseTitle": "New Anime Japanese",
              "synopsis": "This is an E2E testing anime.",
              "largeImage": "https://example.com/img/${animeId}_large.jpg",
              "rating": "PG-13",
              "nsfw": "SFW"
            }
        """.trimIndent()

        response = template.post("$animeServer/anime", "application/json", newAnimeJson)
        assert(response.contains("anime created"))

        response = template.get("$animeServer/anime", "application/json", Pair("id", animeId))
        assert(response.contains("New E2E Test Anime"))

        val pollServer = "http://localhost:8892"

        response = template.get(pollServer, "application/json")
        assertEquals("Noop!", response)

        val topicId = "707"
        val pollOptionId = "1"
        val newPollJson = """
            {
              "contentId": $animeId,
              "contentType": "anime",
              "topicId": $topicId,
              "pollOptionId": $pollOptionId,
              "title": "Which character is the best?",
              "episode": 1,
              "votes": 1250
            }
        """.trimIndent()

        response = template.post("$pollServer/poll", "application/json", newPollJson)
        assert(response.contains("poll created"))

        response = template.get(
            "$pollServer/poll",
            "application/json",
            Pair("contentId", animeId),
            Pair("contentType", "anime"),
            Pair("topicId", topicId),
            Pair("pollOptionId", pollOptionId)
        )
        assert(response.contains("Which character is the best?"))
        assert(response.contains("poll info"))
    }

    /// Test Support

    private fun waitUntilReady(url: String, timeoutMillis: Long = 10_000) {
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (System.currentTimeMillis() < deadline) {
            try {
                if (template.get(url, "application/json") == "Noop!") return
            } catch (_: Exception) {
                Thread.sleep(100)
            }
        }
        throw IllegalStateException("Server at $url was not ready within ${timeoutMillis}ms")
    }

    private fun runCommand(port: Int, services: String, command: String, workingDir: File): Process {
        val builder = ProcessBuilder(*command.split(" ").toTypedArray())
            .directory(workingDir)
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
        builder.environment()["PORT"] = port.toString()
        builder.environment()["DATABASE_URL"] =
            "jdbc:mysql://localhost:3306/test_${services}?user=uservices&password=uservices"
        builder.environment()["REDIS_HOST"] = "localhost"
        builder.environment()["REDIS_PASSWORD"] = "foobared"
        builder.environment()["DISCOVERY_SERVER_ENDPOINT"] = "http://localhost:8890"
        return builder.start()
    }
}
