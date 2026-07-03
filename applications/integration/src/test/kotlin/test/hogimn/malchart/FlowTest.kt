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
    lateinit var allocations: Process
    lateinit var backlog: Process
    lateinit var registration: Process
    lateinit var timesheets: Process

    @Before
    fun setUp() {
        val userDir = System.getProperty("user.dir")

        RedisConfig().getClient("localhost", "foobared").flushAll()

        discovery = runCommand(
            8880,
            "discovery",
            "java -jar $userDir/../discovery-server/build/libs/discovery-server.jar",
            File(userDir)
        )

        JdbcTemplate(DataSourceConfig().createDataSource("jdbc:mysql://localhost:3306/allocations_test?user=uservices&password=uservices")).apply {
            execute("delete from allocations")
        }
        JdbcTemplate(DataSourceConfig().createDataSource("jdbc:mysql://localhost:3306/backlog_test?user=uservices&password=uservices")).apply {
            execute("delete from stories")
        }
        JdbcTemplate(DataSourceConfig().createDataSource("jdbc:mysql://localhost:3306/registration_test?user=uservices&password=uservices")).apply {
            execute("delete from projects")
            execute("delete from accounts")
            execute("delete from users")
        }
        JdbcTemplate(DataSourceConfig().createDataSource("jdbc:mysql://localhost:3306/timesheets_test?user=uservices&password=uservices")).apply {
            execute("delete from time_entries")
        }

        allocations = runCommand(8881, "allocations", "java -jar $userDir/../allocations-server/build/libs/allocations-server.jar", File(userDir))
        backlog = runCommand(8882, "backlog", "java -jar $userDir/../backlog-server/build/libs/backlog-server.jar", File(userDir))
        registration = runCommand(8883, "registration","java -jar $userDir/../registration-server/build/libs/registration-server.jar", File(userDir))
        timesheets = runCommand(8884, "timesheets", "java -jar $userDir/../timesheets-server/build/libs/timesheets-server.jar", File(userDir))
    }

    @After
    fun tearDown() {
        discovery.destroy()
        allocations.destroy()
        backlog.destroy()
        registration.destroy()
        timesheets.destroy()
    }

    @Test
    fun testBasicFlow() {
        listOf(8880, 8881, 8882, 8883, 8884).forEach { waitUntilReady("http://localhost:$it") }

        var response: String?

        val discoveryServer = "http://localhost:8880"
        response = template.get(discoveryServer, "application/json")
        assertEquals("Noop!", response)



        val registrationServer = "http://localhost:8883"

        response = template.get(registrationServer, "application/json")
        assertEquals("Noop!", response)

        response = template.post("$registrationServer/registration", "application/json", """{"name": "aUser"}""")
        val aUserId = findResponseId(response)
        assert(aUserId.toLong() > 0)

        response = template.get("$registrationServer/users", "application/json", Pair("userId", aUserId))
        assert(response.isNotEmpty())

        response = template.get("$registrationServer/accounts", "application/json", Pair("ownerId", aUserId))
        val anAccountId = findResponseId(response)
        assert(anAccountId.toLong() > 0)

        response = template.post(
            "$registrationServer/projects",
            "application/vnd.appcontinuum.v2+json",
            """{"accountId":"$anAccountId","name":"aProject","active":true,"funded":true}"""
        )
        val aProjectId = findResponseId(response)
        assert(aProjectId.toLong() > 0)

        response = template.get(
            "$registrationServer/projects",
            "application/vnd.appcontinuum.v2+json",
            Pair("accountId", anAccountId)
        )
        assert(response.isNotEmpty())



        val allocationsServer = "http://localhost:8881"

        response = template.get(allocationsServer, "application/json")
        assertEquals("Noop!", response)

        response = template.post(
            "$allocationsServer/allocations",
            "application/json",
            """{"projectId":$aProjectId,"userId":$aUserId,"firstDay":"2015-05-17","lastDay":"2015-05-26"}"""
        )
        val anAllocationId = findResponseId(response)
        assert(anAllocationId.toLong() > 0)

        response = template.get("$allocationsServer/allocations", "application/json", Pair("projectId", aProjectId))
        assert(response.isNotEmpty())



        val backlogServer = "http://localhost:8882"

        response = template.get(backlogServer, "application/json")
        assertEquals("Noop!", response)

        response = template.post(
            "$backlogServer/stories",
            "application/json",
            """{"projectId":$aProjectId,"name":"A story"}"""
        )
        val aStoryId = findResponseId(response)
        assert(aStoryId.toLong() > 0)

        response = template.get("$backlogServer/stories", "application/json", Pair("projectId", aProjectId))
        assert(response.isNotEmpty())



        val timesheetsServer = "http://localhost:8884"

        response = template.get(timesheetsServer, "application/json")
        assertEquals("Noop!", response)

        response = template.post(
            "$timesheetsServer/time-entries",
            "application/json",
            """{"projectId":$aProjectId,"userId":$aUserId,"date":"2015-05-17","hours":"8"}"""
        )
        val aTimeEntryId = findResponseId(response)
        assert(aTimeEntryId.toLong() > 0)

        response = template.get("$timesheetsServer/time-entries", "application/json", Pair("userId", aUserId))
        assert(response.isNotEmpty())
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

    private fun findResponseId(response: String): String {
        return Regex("id\":(\\d+),").find(response)?.groupValues!![1]
    }

    private fun runCommand(port: Int, services: String, command: String, workingDir: File): Process {
        val builder = ProcessBuilder(*command.split(" ").toTypedArray())
            .directory(workingDir)
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
        builder.environment()["PORT"] = port.toString()
        builder.environment()["DATABASE_URL"] =
            "jdbc:mysql://localhost:3306/${services}_test?user=uservices&password=uservices"
        builder.environment()["REDIS_HOST"] = "localhost"
        builder.environment()["REDIS_PASSWORD"] = "foobared"
        builder.environment()["DISCOVERY_SERVER_ENDPOINT"] = "http://localhost:8880"
        return builder.start()
    }
}
