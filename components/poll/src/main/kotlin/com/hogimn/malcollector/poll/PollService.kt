package com.hogimn.malcollector.poll

class PollService(val gateway: PollDataGateway) {

    fun getPoll(contentId: Int, contentType: String, topicId: Int, pollOptionId: Int): PollInfo {
        val record = gateway.findObject(contentId, contentType, topicId, pollOptionId)
        return record?.toPollInfo("poll info")
            ?: throw IllegalStateException(
                "Poll with id $contentId contentType $contentType" +
                        " pollOptionId $pollOptionId topicId $topicId not found"
            )
    }

    fun getPollsByContent(contentId: Int, contentType: String): List<PollInfo> {
        return gateway.findByContentId(contentId, contentType).map { it.toPollInfo("poll info") }
    }

    fun createPoll(request: PollInfo): PollInfo {
        val record = gateway.create(
            contentId = request.contentId,
            contentType = request.contentType,
            topicId = request.topicId,
            pollOptionId = request.pollOptionId,
            title = request.title,
            episode = request.episode,
            votes = request.votes
        )
        return record.toPollInfo("poll created")
    }

    fun updatePoll(request: PollInfo): PollInfo {
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
            val record =
                gateway.findObject(request.contentId, request.contentType, request.topicId, request.pollOptionId)!!
            return record.toPollInfo("poll updated")
        } else {
            throw IllegalStateException(
                "Poll with id ${request.contentId} pollOptionId ${request.pollOptionId}" +
                        " topicId ${request.topicId} not found to update"
            )
        }
    }

    fun upsertPoll(request: PollInfo): PollInfo {
        val record = gateway.upsert(
            contentId = request.contentId,
            contentType = request.contentType,
            topicId = request.topicId,
            pollOptionId = request.pollOptionId,
            title = request.title,
            episode = request.episode,
            votes = request.votes
        )
        return record.toPollInfo("poll upserted")
    }

    fun getContentIds(contentType: String): List<Int> {
        return gateway.findDistinctContentIds(contentType)
    }

    fun getPollSummary(contentId: Int, contentType: String): PollSummaryInfo {
        val records = gateway.findByContentId(contentId, contentType)

        if (records.isEmpty()) {
            throw IllegalStateException("No poll records found for contentId $contentId, contentType $contentType")
        }

        val distribution = buildEpisodeDistribution(records)

        return PollSummaryInfo(
            contentId = contentId,
            contentType = contentType,
            episodeDistribution = distribution
        )
    }

    fun getPollSummaries(contentIds: List<Int>, contentType: String): List<PollSummaryInfo> {
        if (contentIds.isEmpty()) return emptyList()

        val records = gateway.findByContentIds(contentIds, contentType)

        return records.groupBy { it.contentId }
            .map { (contentId, contentRecords) ->
                PollSummaryInfo(
                    contentId = contentId,
                    contentType = contentType,
                    episodeDistribution = buildEpisodeDistribution(contentRecords)
                )
            }
    }

    private fun buildEpisodeDistribution(records: List<PollRecord>): Map<Int, Map<String, Any>> {
        val groupedByEpisode = records.groupBy { it.episode }

        return groupedByEpisode.mapValues { (_, episodeRecords) ->
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