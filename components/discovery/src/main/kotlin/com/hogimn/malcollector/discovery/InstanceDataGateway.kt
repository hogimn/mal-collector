package com.hogimn.malcollector.discovery

import redis.clients.jedis.UnifiedJedis
import redis.clients.jedis.params.SetParams

class InstanceDataGateway(val client: UnifiedJedis, val timeToLiveInMillis: Long) {
    fun heartbeat(appId: String, url: String): InstanceRecord {
        client.set("$appId:$url", url, SetParams().px(timeToLiveInMillis))
        return InstanceRecord(appId, url)
    }

    fun findBy(appId: String): List<InstanceRecord> {
        return client.keys("$appId:*")
            .map { client.get(it) }
            .map { InstanceRecord(appId, it) }
    }
}