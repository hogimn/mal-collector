package com.hogimn.malcollector.redissupport

import redis.clients.jedis.DefaultJedisClientConfig
import redis.clients.jedis.HostAndPort
import redis.clients.jedis.RedisClient
import redis.clients.jedis.UnifiedJedis


open class RedisConfig {
    fun getClient(host: String, password: String, port: Int = 6379): UnifiedJedis {
        val config = DefaultJedisClientConfig.builder()
            .password(password)
            .timeoutMillis(2000)
            .build()
        return RedisClient.builder()
            .hostAndPort(HostAndPort(host, port))
            .clientConfig(config)
            .build()
    }
}
