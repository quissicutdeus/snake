package com.quissicutdeus.sniddle.ui

import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import com.quissicutdeus.sniddle.game.Difficulty
import com.quissicutdeus.sniddle.game.model.SnailStyle
import com.quissicutdeus.sniddle.game.sim.GameSim
import com.quissicutdeus.sniddle.game.sim.SecondPlayer
import com.quissicutdeus.sniddle.game.sim.SimOptions
import com.quissicutdeus.sniddle.game.sim.Thumbstick

/** How one kid's two-player game went, for the end screen, the high scores and their unlocks. */
data class KidResult(
    /** Their player id on this phone, or [GUEST_ID]. */
    val playerId: Int,
    val name: String,
    val style: SnailStyle,
    /** Best length this game (through any comebacks). */
    val score: Int,
    val snailsEaten: Int,
    val biggestSeconds: Int,
    val timesGobbled: Int,
) {
    /** The same as a one-player [GameResult] from [game], for the kid's unlocks. */
    fun asGameResult(game: TogetherResult) =
        GameResult(score, snailsEaten, style, game.timestamp, biggestSeconds = biggestSeconds, littleKid = game.littleKid)
}

/** How a two-player game went: the left kid first. */
data class TogetherResult(
    val kids: List<KidResult>,
    val timestamp: Long,
    /** Played gently (both kids are in little kid mode): their high scores get a 🐣. */
    val littleKid: Boolean = false,
)

/**
 * A two-player game: the world, and a [Seat] for each kid's half of the
 * screen, run by the shared [GameLoop]. Not saved: if Android closes the app
 * in the background during a two-player game, the kids come back to the
 * title screen (see the app's screen saver).
 */
internal class TwoPlayerGame(
    /** Left kid first. */
    val kids: List<PlayerCard>,
    newStick: () -> Thumbstick,
    seconds: Float,
    /** Today's holidays and arena, and how gentle (see [Profile.togetherOptions]). */
    options: SimOptions,
) {
    val sim =
        GameSim(
            playerStyle = kids[0].style,
            playerName = kids[0].label,
            options = options,
            secondPlayer = SecondPlayer(kids[1].label, kids[1].style, seconds),
        )

    /** Each kid's half of the screen, left first. */
    val halves = List(2) { Seat(newStick()) }

    /** After the clock runs out, "Time's up!" shows a moment before the end screen. */
    val loop = GameLoop(sim, halves, TIME_UP_DELAY_SECONDS, ::result)

    /** Which half each finger started in: it steers that half's snail even if it drifts across. */
    private val owner = HashMap<PointerId, Int>()

    /** Seconds left on the shared clock. */
    val timeLeft: Float get() = sim.together!!.timeLeft

    private fun result(): TogetherResult {
        val together = sim.together!!
        val kidResults =
            kids.mapIndexed { i, card ->
                val s = sim.players[i]
                val state = together.kids[i]
                KidResult(card.id, card.label, card.style, s.score, s.snailsEaten, state.biggestSeconds.toInt(), state.timesGobbled)
            }
        return TogetherResult(kidResults, System.currentTimeMillis(), littleKid = sim.difficulty == Difficulty.LITTLE)
    }

    /**
     * Hands each finger to the half it started in ([width] is the whole width
     * in pixels), then steers each half from its own fingers.
     */
    fun steerHalves(
        changes: List<PointerInputChange>,
        width: Int,
    ) {
        for (c in changes) {
            if (c.pressed) owner.getOrPut(c.id) { if (c.position.x < width / 2f) 0 else 1 }
        }
        halves.forEachIndexed { i, half -> half.steerWith(changes.filter { owner[it.id] == i }) }
        for (c in changes) if (!c.pressed) owner.remove(c.id)
    }

    private companion object {
        const val TIME_UP_DELAY_SECONDS = 2f
    }
}
