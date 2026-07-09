package com.hogimn.malcollector.malsupport

import dev.katsute.mal4j.MyAnimeList
import dev.katsute.mal4j.property.ExperimentalFeature

class MalProvider(clientId: String) {
    private val myAnimeList = MyAnimeList.withClientID(clientId).apply {
        enableExperimentalFeature(ExperimentalFeature.ALL)
    }

    fun getMyAnimeList(): MyAnimeList = myAnimeList
}