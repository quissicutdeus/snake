package com.quissicutdeus.sniddle.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.quissicutdeus.sniddle.Storage
import com.quissicutdeus.sniddle.game.model.Stats

/**
 * What this device has been told, for everyone who plays on it (not one kid):
 * the "What's new" card. (First-game tips are per kid: see [tipsDue].)
 */
class DeviceNotes(
    private val storage: Storage,
    allNews: List<News> = WhatsNew,
) {
    init {
        // First launch of a version with news: a fresh install starts with
        // everything already "seen"; one that was played on before sees it all.
        if (storage.newsSeen == null) storage.newsSeen = if (playedBefore()) 0 else allNews.maxOfOrNull { it.number } ?: 0
    }

    /** News not shown on this device yet, newest last. */
    var news: List<News> by mutableStateOf(allNews.filter { it.number > (storage.newsSeen ?: 0) })
        private set

    /** [news] has been on screen, so it won't be shown again after this. */
    fun newsShown() {
        val newest = news.maxOfOrNull { it.number } ?: return
        storage.newsSeen = maxOf(newest, storage.newsSeen ?: 0)
    }

    /** Hides [news] (tapped away, or the kid went to play). */
    fun dismissNews() {
        news = emptyList()
    }

    /** Anyone has finished a game on this device (kids who already know how to play). */
    private fun playedBefore(): Boolean =
        storage.loadScores().isNotEmpty() || storage.loadPlayers().any { storage.player(it).loadStats().gamesPlayed > 0 }
}

/**
 * Whether a kid's next game shows the first-game tips: until they've finished
 * a game. Per kid, so a little brother added as a new player gets them too.
 * (The first kid on an updated phone counts the games from before players
 * existed, so they don't.)
 */
fun tipsDue(kid: Stats): Boolean = kid.gamesPlayed == 0
