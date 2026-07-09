package test.hogimn.malcollector.discovery

import com.hogimn.malcollector.discovery.InstanceDataGateway
import com.hogimn.malcollector.redissupport.RedisConfig
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import redis.clients.jedis.params.SetParams

class InstanceDataGatewayTest {
    val client = RedisConfig().getClient("localhost", "foobared")

    @Before
    fun cleanDatabase() {
        client.flushAll()
    }

    @Test
    fun testHeartbeat() {
        val gateway = InstanceDataGateway(client, 5000L)

        val instance = gateway.heartbeat("allocations", "http://localhost:8081")

        assertEquals("allocations", instance.appId)
        assertEquals("http://localhost:8081", instance.url)
    }

    @Test
    fun testFindBy() {
        client.set("allocations:http://localhost:8081", "http://localhost:8081", SetParams().px(5000L))

        val gateway = InstanceDataGateway(client, 5000L)

        val instance = gateway.findBy("allocations").first()

        assertEquals("allocations", instance.appId)
        assertEquals("http://localhost:8081", instance.url)
    }

    @Test
    fun testExpired() {
        val gateway = InstanceDataGateway(client, 5L)

        gateway.heartbeat("allocations", "http://localhost:8081")

        Thread.sleep(10)

        assertEquals(0, gateway.findBy("allocations").size)
    }
}
