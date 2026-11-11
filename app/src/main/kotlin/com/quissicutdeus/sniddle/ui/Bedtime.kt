package com.quissicutdeus.sniddle.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.quissicutdeus.sniddle.PlayClock
import com.quissicutdeus.sniddle.Storage
import com.quissicutdeus.sniddle.game.model.ScoreEntry
import com.quissicutdeus.sniddle.game.model.Unlock
import com.quissicutdeus.sniddle.game.sim.GameSim
import com.quissicutdeus.sniddle.game.sim.Scoreboard
import com.quissicutdeus.sniddle.game.sim.dangers
import java.time.LocalDate

/** The game bedtime stopped, for the rest screen: how it went and what it unlocked. */
data class BedtimeGame(
    val result: GameResult,
    val unlocked: List<Unlock>,
)

/**
 * The grown-ups' bedtime timer for the whole app: the day's count ([clock])
 * and, as Compose state, whether it's time to rest. The limit itself lives in
 * the shared [Settings] (see [Profile.settings]).
 */
class Bedtime(
    private val storage: Storage,
    private val me: Profile,
    today: () -> LocalDate = LocalDate::now,
) {
    val clock = PlayClock({ me.settings.bedtimeMinutes }, { storage.playDay }, { storage.playDay = it }, today)

    /** The daily limit in minutes (0 = off). */
    val limitMinutes: Int get() = me.settings.bedtimeMinutes

    /** There's a limit and today's play has used it all: the rest screen shows instead of the title. */
    var timeUp: Boolean by mutableStateOf(clock.timeUp)
        private set

    /** The game the timer just stopped (shown on the rest screen until the app is closed). */
    var lastGame: BedtimeGame? by mutableStateOf(null)
        private set

    /** Looks again (a new day, a game played, a grown-up's change). Returns [timeUp]: then Play goes home, which shows the rest screen. */
    fun refresh(): Boolean {
        timeUp = clock.timeUp
        return timeUp
    }

    /** A grown-up sets the daily limit (0 = off). */
    fun setLimit(minutes: Int) {
        me.changeSettings(me.settings.copy(bedtimeMinutes = minutes))
        clock.settle()
        refresh()
    }

    /** A grown-up gives [EXTRA_MINUTES] more today, from the rest screen. */
    fun grantMore() {
        clock.grant(EXTRA_MINUTES)
        lastGame = null
        refresh()
    }

    /** The timer for a game; [onStopped] runs after it stops one (see [gameStopped]). */
    fun forGame(onStopped: () -> Unit) =
        GameBedtime<GameResult>(clock) {
            gameStopped(it)
            onStopped()
        }

    /**
     * The timer for a two-player game. A game it stops counts for both kids
     * through [record] (stats, unlocks, high scores), and the rest screen
     * shows how it went for the kid picked on the title screen; then
     * [onStopped] runs, as for one kid.
     */
    fun forTogether(
        record: (TogetherResult) -> List<List<Unlock>>,
        onStopped: () -> Unit,
    ) = GameBedtime<TogetherResult>(clock) { result ->
        clock.flush()
        val unlocked = record(result)
        lastGame = BedtimeGame(result.kids[0].asGameResult(result), unlocked.firstOrNull().orEmpty())
        refresh()
        onStopped()
    }

    /**
     * Bedtime stopped a game: it counts like any finished game (stats and
     * unlocks), and a high score goes on the list under the snail's name
     * (a snail with no name yet has no name to put there, so it doesn't).
     */
    private fun gameStopped(result: GameResult) {
        clock.flush()
        val unlocked = me.recordGame(result)
        if (me.snailName.isNotBlank() && Scoreboard.qualifies(storage.loadScores(), result.score)) {
            val name = Scoreboard.cleanName(me.snailName)
            storage.addScore(ScoreEntry(name, result.score, result.snailsEaten, result.style, result.timestamp))
        }
        lastGame = BedtimeGame(result, unlocked)
        refresh()
    }

    companion object {
        /** What "more time" from the rest screen gives. */
        const val EXTRA_MINUTES = 10

        /** The limits a grown-up can pick, in minutes a day (0 = off). */
        val CHOICES = listOf(0, 15, 30, 45, 60, 90)
    }
}

/** What a game needs from the bedtime timer: the [clock] to count on, and [onStop] for a game it stopped. */
class GameBedtime<R>(
    val clock: PlayClock,
    val onStop: (R) -> Unit,
)

/**
 * The bedtime timer during one game: counts the play, gives a heads-up when
 * [WARN_MS] is left, and once time's up asks the game to stop at the first
 * calm moment (see [calm]), or after [HARD_STOP_SECONDS] whatever happens.
 */
class BedtimeInGame<R>(
    var hooks: GameBedtime<R>,
) {
    /** The heads-up to show, if any. */
    var note: String? by mutableStateOf(null)
        private set

    /** The game is paused or over: no heads-up then. */
    var hidden: Boolean by mutableStateOf(false)
        private set

    private var noteSeconds = 0f
    private var warned = false
    private var overtime = 0f

    /**
     * One frame. Once time's up and the moment is calm, hands [stop] the
     * game's [GameBedtime.onStop] to end the game with.
     */
    fun onFrame(
        sim: GameSim,
        dt: Float,
        paused: Boolean,
        stop: ((R) -> Unit) -> Unit,
    ) {
        val running = !paused && !sim.gameOver
        hidden = !running
        hooks.clock.onFrame(dt, running)
        if (!running) return
        if (noteSeconds > 0f) {
            noteSeconds -= dt
            if (noteSeconds <= 0f) note = null
        }
        val left = hooks.clock.leftMs() ?: Long.MAX_VALUE
        when {
            // Ready to warn again (no limit, or a grown-up gave more time).
            left > WARN_MS -> {
                warned = false
            }

            left > 0L -> {
                warn(left)
            }

            else -> {
                overtime += dt
                if (overtime >= HARD_STOP_SECONDS || calm(sim)) stop(hooks.onStop)
            }
        }
    }

    private fun warn(left: Long) {
        if (warned) return
        warned = true
        val minutes = ((left + PlayClock.MS_PER_MINUTE - 1) / PlayClock.MS_PER_MINUTE).toInt()
        note = if (minutes == 1) "1 minute left!" else "$minutes minutes left!"
        noteSeconds = NOTE_SECONDS
    }

    /**
     * Not mid-fight: no player is blinking from a bump (or waiting to come
     * back, with two kids) and no bigger snail is coming for any of them.
     */
    private fun calm(sim: GameSim): Boolean = sim.players.all { p -> p.alive && p.invulnerable <= 0f && sim.dangers(p).isEmpty() }

    companion object {
        /** The heads-up comes this long before time's up. */
        const val WARN_MS = 2 * PlayClock.MS_PER_MINUTE

        /** How long the heads-up stays on screen. */
        private const val NOTE_SECONDS = 5f

        /** Once time's up, the longest a game waits for a calm moment. */
        private const val HARD_STOP_SECONDS = 60f
    }
}

/**
 * The bedtime timer for game [key] (none without [hooks]); played time is
 * written down when the app goes to the background and when the game screen
 * goes away.
 */
@Composable
internal fun <R> rememberBedtimeInGame(
    key: Int,
    hooks: GameBedtime<R>?,
): BedtimeInGame<R>? {
    val nap = remember(key, hooks?.clock) { hooks?.let { BedtimeInGame(it) } }
    // The newest hooks (they're made afresh each time the app redraws).
    if (hooks != null) nap?.hooks = hooks
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { nap?.hooks?.clock?.flush() }
    DisposableEffect(nap) { onDispose { nap?.hooks?.clock?.flush() } }
    return nap
}

/** "2 minutes left!" at the top of the game, under the pause button. */
@Composable
internal fun BedtimeNote(
    nap: BedtimeInGame<*>?,
    modifier: Modifier = Modifier,
) {
    val text = nap?.note ?: return
    if (nap.hidden) return
    Row(
        modifier
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(top = 72.dp)
            .clip(RoundedCornerShape(50))
            .background(KidColors.Sunny)
            .padding(horizontal = 18.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("😴", fontSize = 24.sp)
        Text(
            text,
            style = MaterialTheme.typography.titleLarge,
            color = KidColors.Ink,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}
