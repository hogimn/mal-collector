package test.hogimn.malcollector.redissupport

import com.hogimn.malcollector.redissupport.RedisConfig
import org.junit.Test
import kotlin.test.assertEquals

class RedisConfigTest {
    val client = RedisConfig().getClient("localhost", "foobared")

    @Test
    fun testFind() {
        client.set("aKey", "aValue")
        assertEquals("aValue", client.get("aKey"))
    }
}
