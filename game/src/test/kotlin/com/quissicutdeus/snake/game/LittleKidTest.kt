package com.quissicutdeus.snake.game

import com.quissicutdeus.snake.game.model.Eyes
import com.quissicutdeus.snake.game.model.Hat
import com.quissicutdeus.snake.game.model.ScoreEntry
import com.quissicutdeus.snake.game.model.SnakeStyle
import com.quissicutdeus.snake.game.model.dist
import com.quissicutdeus.snake.game.sim.AiMode
import com.quissicutdeus.snake.game.sim.GameEvent
import com.quissicutdeus.snake.game.sim.GameSim
import com.quissicutdeus.snake.game.sim.SavedRun
import com.quissicutdeus.snake.game.sim.Scoreboard
import com.quissicutdeus.snake.game.sim.SimOptions
import com.quissicutdeus.snake.game.sim.SizeRelation
import com.quissicutdeus.snake.game.sim.SteerInput
import com.quissicutdeus.snake.game.sim.compareSize
import kotlin.math.atan2
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Little kid mode: the same game, just gentler. */
class LittleKidTest {
    /** What happened to the player over some minutes of play. */
    private data class Tally(
        var seconds: Int = 0,
        /** Hearts lost, the last one (being gobbled) included. */
        var heartsLost: Int = 0,
        var gobbled: Int = 0,
        /** Bigger snakes within [NEAR] of the player, added up once a second. */
        var biggerNear: Int = 0,
        /** Seconds some computer snake spent chasing the player, added up once a second. */
        var chasedSeconds: Int = 0,
        var bestScore: Int = 0,
    )

    /**
     * A little kid at the controls: heads for the nearest food dot and pays
     * no attention to other snakes at all. Plays the first [seconds] of
     * [games] games (or less, when gobbled), where a small snake is most at risk.
     */
    private fun play(
        difficulty: Difficulty,
        games: Int,
        seconds: Int,
    ): Tally {
        val t = Tally()
        for (seed in 1L..games) {
            val sim = GameSim(seed = seed, options = SimOptions(difficulty = difficulty))
            playOneGame(sim, t.seconds + seconds, t)
        }
        return t
    }

    private fun playOneGame(
        sim: GameSim,
        seconds: Int,
        t: Tally,
    ) {
        val p = sim.player!!
        val input = SteerInput()
        var step = 0
        while (t.seconds < seconds && !sim.gameOver) {
            if (step % STEER_EVERY == 0) {
                sim.world.pellets
                    .minByOrNull { dist(p.x, p.y, it.x, it.y) }
                    ?.let { input.set(atan2(it.y - p.y, it.x - p.x)) }
            }
            sim.step(input)
            for (e in sim.events) {
                if (e is GameEvent.Hurt && e.snakeId == p.id) t.heartsLost++
                if (e is GameEvent.PlayerEaten) t.gobbled++
            }
            sim.events.clear()
            if (++step % STEPS_PER_SECOND == 0) lookAround(sim, t)
            t.bestScore = maxOf(t.bestScore, p.score)
        }
        t.heartsLost += if (sim.gameOver) 1 else 0
    }

    /** Once a second: how many bigger snakes are close, and is one chasing the player? */
    private fun lookAround(
        sim: GameSim,
        t: Tally,
    ) {
        val p = sim.player!!
        t.seconds++
        val others = sim.world.snakes.filter { it.alive && !it.isPlayer }
        t.biggerNear += others.count { compareSize(it.length, p.length) == SizeRelation.BIGGER && dist(it.x, it.y, p.x, p.y) < NEAR }
        if (others.any { it.brain?.mode == AiMode.CHASE && it.brain?.targetSnakeId == p.id }) t.chasedSeconds++
    }

    @Test fun littleKidsGetHurtLessButStillCanBe() {
        val normal = play(Difficulty.NORMAL, GAMES, SECONDS)
        val little = play(Difficulty.LITTLE, GAMES, SECONDS)
        println("$GAMES games of up to $SECONDS s:\n  normal $normal\n  little $little")
        assertTrue(little.heartsLost * 2 <= normal.heartsLost, "hurt at most half as often: $little vs $normal")
        assertTrue(little.biggerNear * 3 <= normal.biggerNear * 2, "fewer bigger snakes around: $little vs $normal")
        assertTrue(little.chasedSeconds * 2 <= normal.chasedSeconds, "chased at most half as long: $little vs $normal")
        // Still a game: bigger snakes come by, chase and bite.
        assertTrue(little.heartsLost > 0 && little.biggerNear > 0 && little.chasedSeconds > 0, "still a game: $little")
    }

    @Test fun littleKidModeStartsWithAnExtraHeartAndMoreFood() {
        val normal = GameSim(seed = 4)
        val little = GameSim(seed = 4, options = SimOptions(difficulty = Difficulty.LITTLE))
        assertEquals(GameConfig.PLAYER_HEARTS, normal.player!!.hearts)
        assertEquals(GameConfig.PLAYER_HEARTS + 1, little.player!!.hearts)
        assertEquals(GameConfig.PELLET_COUNT, normal.world.pellets.size)
        assertTrue(little.world.pellets.size > normal.world.pellets.size)
    }

    @Test fun aLittleKidGameComesBackJustAsGentle() {
        val sim = GameSim(seed = 6, playerName = "Bea", options = SimOptions(difficulty = Difficulty.LITTLE))
        val text = sim.snapshot()!!.encode()
        val run = assertNotNull(SavedRun.decode(text))
        assertEquals(Difficulty.LITTLE, run.difficulty)
        assertEquals(4, run.hearts)
        // Whatever the screen asks for, the saved game decides.
        val back = GameSim(seed = 7, options = SimOptions(difficulty = Difficulty.NORMAL), resume = run)
        assertEquals(Difficulty.LITTLE, back.difficulty)
        assertEquals(4, back.player!!.hearts)
        // A normal game can't come back with 4 hearts.
        assertNull(SavedRun.decode(run.copy(difficulty = Difficulty.NORMAL).encode()))
    }

    @Test fun aGameSavedBeforeLittleKidModeComesBackNormal() {
        val run = assertNotNull(SavedRun.decode("run2;300.0;320.0;2;1;0.0,0.0,0.0;4.5;1,2,STRIPES,GOOGLY,NONE;Me; too"))
        assertEquals(Difficulty.NORMAL, run.difficulty)
        assertEquals(4.5f, run.biggestSeconds)
        assertEquals("Me; too", run.name)
        assertNull(SavedRun.decode("run3;300.0;320.0;2;1;0.0,0.0,0.0;4.5;sideways;1,2,STRIPES,GOOGLY,NONE;Me"))
    }

    @Test fun littleKidScoresAreMarkedAndOldScoresStillLoad() {
        val style = SnakeStyle(color1 = 3, color2 = 7, eyes = Eyes.GOOGLY, hat = Hat.CROWN)
        val big = ScoreEntry("Caleb", 120, 4, style, 1000L)
        val small = ScoreEntry("Bea", 90, 1, style, 2000L, littleKid = true)
        val text = Scoreboard.encode(listOf(big, small))
        assertEquals(listOf(big, small), Scoreboard.decode(text))
        // A list saved before little kid mode: five fields a line, nobody marked.
        val old = Scoreboard.decode("120\t4\t1000\t${style.encode()}\tCaleb\n90\t1\t2000\t${style.encode()}\tBea")
        assertEquals(listOf(big, small.copy(littleKid = false)), old)
        // An older version reads only the first five fields, so the new list still opens there.
        val plain = Scoreboard.encode(listOf(small.copy(littleKid = false)))
        assertTrue(Scoreboard.encode(listOf(small)).startsWith(plain + "\t"), "the mark only adds a field at the end")
    }

    private companion object {
        const val STEPS_PER_SECOND = 60
        const val STEER_EVERY = 15
        const val NEAR = 900f
        const val GAMES = 12
        const val SECONDS = 60
    }
}
