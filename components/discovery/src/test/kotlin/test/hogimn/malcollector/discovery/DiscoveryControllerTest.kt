package test.hogimn.malcollector.discovery

import com.fasterxml.jackson.core.type.TypeReference
import com.hogimn.malcollector.discovery.DiscoveryController
import com.hogimn.malcollector.discovery.InstanceDataGateway
import com.hogimn.malcollector.discovery.InstanceInfo
import com.hogimn.malcollector.redissupport.RedisConfig
import com.hogimn.malcollector.restsupport.BasicServer
import com.hogimn.malcollector.testsupport.TestControllerSupport
import org.junit.After
import org.junit.Before
import org.junit.Test
import redis.clients.jedis.params.SetParams
import kotlin.test.assertEquals

class DiscoveryControllerTest : TestControllerSupport() {
    val client = RedisConfig().getClient("localhost", "foobared")

    private val server = object : BasicServer(8081) {
        override fun registerContexts() {
            context("/discovery/apps", DiscoveryController(mapper, InstanceDataGateway(client, 5000)))
        }
    }

    @Before
    fun setUp() {
        client.flushAll()
        server.start()
    }

    @After
    fun tearDown() {
        server.stop()
    }

    @Test
    fun testHeartbeat() {
        val json = "{\"appId\":\"allocations\",\"url\":\"http://localhost:8081\"}"
        val response = template.post("http://localhost:8081/discovery/apps", "application/json", json)
        val actual = mapper.readValue(response, InstanceInfo::class.java)

        assertEquals("allocations", actual.appId)
        assertEquals("http://localhost:8081", actual.url)
    }

    @Test
    fun testFind() {
        client.set("allocations:http://localhost:8081", "http://localhost:8081", SetParams().px(5000L))
        client.set("allocations:http://localhost:8082", "http://localhost:8083", SetParams().px(5000L))
        client.set("allocations:http://localhost:8083", "http://localhost:8083", SetParams().px(5000L))

        val response =
            template.get("http://localhost:8081/discovery/apps", "application/json", Pair("appId", "allocations"))

        val instances: List<InstanceInfo> = mapper.readValue(response, object : TypeReference<List<InstanceInfo>>() {})
        assertEquals(3, instances.size)

        val first = instances.first()
        assertEquals("allocations", first.appId)
        assertEquals("http://localhost:8081", first.url)
    }

    @Test
    fun testMixed() {
        client.set("allocations:http://localhost:8081", "http://localhost:8081", SetParams().px(5000L))
        client.set("backlog:http://localhost:8082", "http://localhost:8083", SetParams().px(5000L))
        client.set("timesheets:http://localhost:8083", "http://localhost:8083", SetParams().px(5000L))
        client.set("registration:http://localhost:8084", "http://localhost:8084", SetParams().px(5000L))

        val response =
            template.get("http://localhost:8081/discovery/apps", "application/json", Pair("appId", "allocations"))

        val instances: List<InstanceInfo> = mapper.readValue(response, object : TypeReference<List<InstanceInfo>>() {})
        assertEquals(1, instances.size)
    }

    @Test
    fun testEmpty() {
        val response =
            template.get("http://localhost:8081/discovery/apps", "application/json", Pair("appId", "allocations"))

        val instances: List<InstanceInfo> = mapper.readValue(response, object : TypeReference<List<InstanceInfo>>() {})
        assertEquals(0, instances.size)
    }
}
