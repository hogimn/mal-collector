package com.hogimn.malcollector.poll

data class PollSummaryInfo(
    val contentId: Int,
    val contentType: String,
    val episodeDistribution: Map<Int, Map<String, Any>>
)