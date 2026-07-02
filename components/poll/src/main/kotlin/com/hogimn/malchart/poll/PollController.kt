package com.hogimn.malchart.poll

import com.fasterxml.jackson.databind.ObjectMapper
import com.sun.net.httpserver.HttpExchange
import com.hogimn.malchart.restsupport.BasicController

class PollController(val mapper: ObjectMapper, val gateway: PollDataGateway) : BasicController() {

    override fun handle(exchange: HttpExchange): Boolean {
        val mediaTypes = listOf("application/json", "application/vnd.malchart.v1+json")

        return get(exchange, "/poll", mediaTypes) {
            val contentId = parameters(exchange)["contentId"]!!
            val topicId = parameters(exchange)["topicId"]!!
            val pollOptionId = parameters(exchange)["pollOptionId"]!!
            val record = gateway.findObject(contentId.toInt(), topicId.toInt(), pollOptionId.toInt())
            if (record != null) {
                mapper.writeValueAsString(record.toPollInfo("poll info"))
            } else {
                throw IllegalStateException("Poll with id $contentId pollOptionId $pollOptionId " +
                        "topicId $topicId not found")
            }
        } || post(exchange, "/poll", mediaTypes) {
            val request = mapper.readValue(body(exchange), PollInfo::class.java)

            val record = gateway.create(
                contentId = request.contentId,
                topicId = request.topicId,
                pollOptionId = request.pollOptionId,
                title = request.title,
                episode = request.episode,
                votes = request.votes
            )

            mapper.writeValueAsString(record.toPollInfo("poll created"))
        } || put(exchange, "/poll", mediaTypes) {
            val request = mapper.readValue(body(exchange), PollInfo::class.java)

            val updatedCount = gateway.update(
                contentId = request.contentId,
                topicId = request.topicId,
                pollOptionId = request.pollOptionId,
                title = request.title,
                episode = request.episode,
                votes = request.votes
            )

            if (updatedCount > 0) {
                val record = gateway.findObject(request.contentId, request.topicId, request.pollOptionId)!!
                mapper.writeValueAsString(record.toPollInfo("poll updated"))
            } else {
                throw IllegalStateException("Poll with id ${request.contentId} pollOptionId " +
                        "${request.pollOptionId} topicId ${request.topicId} not found to update")
            }
        }
    }

    private fun PollRecord.toPollInfo(info: String): PollInfo {
        return PollInfo(
            contentId = this.contentId,
            topicId = this.topicId,
            pollOptionId = this.pollOptionId,
            title = this.title,
            episode = this.episode,
            votes = this.votes,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt,
            info = info
        )
    }
}