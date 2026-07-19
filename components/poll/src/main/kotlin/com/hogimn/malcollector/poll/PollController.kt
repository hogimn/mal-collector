package com.hogimn.malcollector.poll

import com.fasterxml.jackson.databind.ObjectMapper
import com.hogimn.malcollector.restsupport.BasicController
import com.sun.net.httpserver.HttpExchange

class PollController(val mapper: ObjectMapper, val gateway: PollDataGateway) : BasicController() {

    override fun handle(exchange: HttpExchange): Boolean {
        val mediaTypes = listOf("application/json", "application/vnd.malcollector.v1+json")

        return get(
            exchange, "/poll", mediaTypes,
            { params ->
                params.containsKey("contentId") && params.containsKey("contentType")
                        && params.containsKey("topicId") && params.containsKey("pollOptionId")
            },
        ) {
            val contentId = parameters(exchange)["contentId"]!!
            val contentType = parameters(exchange)["contentType"]!!
            val topicId = parameters(exchange)["topicId"]!!
            val pollOptionId = parameters(exchange)["pollOptionId"]!!
            val record = gateway.findObject(contentId.toInt(), contentType, topicId.toInt(), pollOptionId.toInt())
            if (record != null) {
                mapper.writeValueAsString(record.toPollInfo("poll info"))
            } else {
                throw IllegalStateException(
                    "Poll with id $contentId contentType $contentType pollOptionId $pollOptionId topicId $topicId not found"
                )
            }
        } || get(
            exchange, "/poll", mediaTypes,
            { params ->
                params.containsKey("contentId") && params.containsKey("contentType")
            },
        ) {
            val contentId = parameters(exchange)["contentId"]!!
            val contentType = parameters(exchange)["contentType"]!!
            val records = gateway.findByContentId(contentId.toInt(), contentType)
            val pollList = records.map { it.toPollInfo("poll info") }
            mapper.writeValueAsString(pollList)
        } || post(exchange, "/poll", mediaTypes) {
            val request = mapper.readValue(body(exchange), PollInfo::class.java)

            val record = gateway.create(
                contentId = request.contentId,
                contentType = request.contentType,
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
                contentType = request.contentType,
                topicId = request.topicId,
                pollOptionId = request.pollOptionId,
                title = request.title,
                episode = request.episode,
                votes = request.votes
            )

            if (updatedCount > -1) {
                val record = gateway.findObject(
                    request.contentId, request.contentType, request.topicId, request.pollOptionId
                )!!
                mapper.writeValueAsString(record.toPollInfo("poll updated"))
            } else {
                throw IllegalStateException(
                    "Poll with id ${request.contentId} pollOptionId " +
                            "${request.pollOptionId} topicId ${request.topicId} not found to update"
                )
            }
        } || post(exchange, "/poll/upsert", mediaTypes) {
            val request = mapper.readValue(body(exchange), PollInfo::class.java)

            val record = gateway.upsert(
                contentId = request.contentId,
                contentType = request.contentType,
                topicId = request.topicId,
                pollOptionId = request.pollOptionId,
                title = request.title,
                episode = request.episode,
                votes = request.votes
            )

            mapper.writeValueAsString(record.toPollInfo("poll upserted"))
        } || get(
            exchange, "/poll/content-ids", mediaTypes,
            { params -> params.containsKey("contentType") }
        ) {
            val contentType = parameters(exchange)["contentType"]!!
            val contentIds = gateway.findDistinctContentIds(contentType)
            mapper.writeValueAsString(contentIds)
        } || get(
            exchange, "/poll/summary", mediaTypes,
            { params -> params.containsKey("contentId") && params.containsKey("contentType") }
        ) {
            val contentId = parameters(exchange)["contentId"]!!.toInt()
            val contentType = parameters(exchange)["contentType"]!!

            val records = gateway.findByContentId(contentId, contentType)

            if (records.isNotEmpty()) {
                val groupedByEpisode = records.groupBy { it.episode }

                val distribution = groupedByEpisode.mapValues { (_, episodeRecords) ->
                    var totalScoreSum = 0.0
                    var totalVotes = 0
                    val scoreCounts = mutableMapOf<String, Int>()

                    for (record in episodeRecords) {
                        val scoreKey = record.pollOptionId.toString()

                        scoreCounts[scoreKey] = record.votes
                        totalVotes += record.votes
                        totalScoreSum += (record.pollOptionId * record.votes)
                    }

                    val averageScore = if (totalVotes > 0) {
                        String.format("%.2f", totalScoreSum / totalVotes)
                    } else {
                        "0.0"
                    }

                    val episodeMap = mutableMapOf<String, Any>()
                    episodeMap["averageScore"] = averageScore
                    episodeMap["votes"] = totalVotes
                    episodeMap.putAll(scoreCounts)

                    episodeMap
                }.toSortedMap()

                val infoResult = PollSummaryInfo(
                    contentId = contentId,
                    contentType = contentType,
                    episodeDistribution = distribution
                )

                mapper.writeValueAsString(infoResult)
            } else {
                throw IllegalStateException("No poll records found for contentId $contentId, contentType $contentType")
            }
        }
    }

    private fun PollRecord.toPollInfo(info: String): PollInfo {
        return PollInfo(
            contentId = this.contentId,
            contentType = this.contentType,
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