package com.hogimn.malcollector.poll

import com.fasterxml.jackson.databind.ObjectMapper
import com.hogimn.malcollector.restsupport.BasicController
import com.sun.net.httpserver.HttpExchange

class PollController(val mapper: ObjectMapper, val pollService: PollService) : BasicController() {

    override fun handle(exchange: HttpExchange): Boolean {
        val mediaTypes = listOf("application/json", "application/vnd.malcollector.v1+json")

        return get(
            exchange, "/poll", mediaTypes,
            { params ->
                params.containsKey("contentId") && params.containsKey("contentType")
                        && params.containsKey("topicId") && params.containsKey("pollOptionId")
            },
        ) {
            val contentId = parameters(exchange)["contentId"]!!.toInt()
            val contentType = parameters(exchange)["contentType"]!!
            val topicId = parameters(exchange)["topicId"]!!.toInt()
            val pollOptionId = parameters(exchange)["pollOptionId"]!!.toInt()

            val infoResult = pollService.getPoll(contentId, contentType, topicId, pollOptionId)
            mapper.writeValueAsString(infoResult)
        } || get(
            exchange, "/poll", mediaTypes,
            { params -> params.containsKey("contentId") && params.containsKey("contentType") },
        ) {
            val contentId = parameters(exchange)["contentId"]!!.toInt()
            val contentType = parameters(exchange)["contentType"]!!

            val infoListResult = pollService.getPollsByContent(contentId, contentType)
            mapper.writeValueAsString(infoListResult)
        } || get(
            exchange, "/poll/summary", mediaTypes,
            { params -> params.containsKey("contentId") && params.containsKey("contentType") }
        ) {
            val contentId = parameters(exchange)["contentId"]!!.toInt()
            val contentType = parameters(exchange)["contentType"]!!

            val infoResult = pollService.getPollSummary(contentId, contentType)
            mapper.writeValueAsString(infoResult)
        } || get(
            exchange, "/poll/summaries", mediaTypes,
            { params -> params.containsKey("contentIds") && params.containsKey("contentType") }
        ) {
            val contentType = parameters(exchange)["contentType"]!!
            val contentIds = parameters(exchange)["contentIds"]!!
                .split(",")
                .mapNotNull { it.trim().toIntOrNull() }

            val summariesResult = pollService.getPollSummaries(contentIds, contentType)
            mapper.writeValueAsString(summariesResult)
        } || get(
            exchange, "/poll/content-ids", mediaTypes,
            { params -> params.containsKey("contentType") }
        ) {
            val contentType = parameters(exchange)["contentType"]!!
            val contentIds = pollService.getContentIds(contentType)
            mapper.writeValueAsString(contentIds)
        } || post(exchange, "/poll", mediaTypes) {
            val request = mapper.readValue(body(exchange), PollInfo::class.java)
            val infoResult = pollService.createPoll(request)
            mapper.writeValueAsString(infoResult)
        } || put(exchange, "/poll", mediaTypes) {
            val request = mapper.readValue(body(exchange), PollInfo::class.java)
            val infoResult = pollService.updatePoll(request)
            mapper.writeValueAsString(infoResult)
        } || post(exchange, "/poll/upsert", mediaTypes) {
            val request = mapper.readValue(body(exchange), PollInfo::class.java)
            val infoResult = pollService.upsertPoll(request)
            mapper.writeValueAsString(infoResult)
        }
    }
}