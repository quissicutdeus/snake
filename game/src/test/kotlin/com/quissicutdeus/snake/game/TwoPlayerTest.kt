package com.quissicutdeus.snake.game

import com.quissicutdeus.snake.game.model.PowerType
import com.quissicutdeus.snake.game.model.PowerUp
import com.quissicutdeus.snake.game.model.SnakeStyle
import com.quissicutdeus.snake.game.model.dist
import com.quissicutdeus.snake.game.sim.GameEvent
import com.quissicutdeus.snake.game.sim.GameSim
import com.quissicutdeus.snake.game.sim.SecondPlayer
import com.quissicutdeus.snake.game.sim.SimOptions
import com.quissicutdeus.snake.game.sim.SteerInput
import com.quissicutdeus.snake.game.sim.dangers
import com.quissicutdeus.snake.game.sim.isOnTop
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** Two kids on one screen: one arena, a snake each, comebacks after a gobble, and a shared clock. */
class TwoPlayerTest {
    private val mya = SecondPlayer("Mya", SnakeStyle.DEFAULT.copy(color1 = 3))

    /** An empty arena set up for two kids, who are placed by hand (see [player]). */
    private fun emptyTwoPlayerSim(seconds: Float = GameConfig.TWO_PLAYER_SECONDS) =
        GameSim(
            seed = 42,
            options = SimOptions(pelletCount = 0, aiCount = 0, spawnPlayer = false, aiEnabled = false, powerUps = false),
            secondPlayer = mya.copy(seconds = seconds),
        )

    private fun steer(heading: Float) = SteerInput().apply { set(heading) }

    @Test fun twoKidsStartSideBySideAndEachSteersTheirOwnSnake() {
        val sim = GameSim(seed = 3, playerName = "Caleb", secondPlayer = mya)
        val (caleb, kid2) = sim.players
        assertEquals(2, sim.players.size)
        assertSame(caleb, sim.player, "the first kid is still the player, for one-player code")
        assertEquals("Mya", kid2.name)
        assertTrue(caleb.isPlayer && kid2.isPlayer && caleb.alive && kid2.alive)
        assertTrue(caleb.x < kid2.x, "the first kid starts on the left")
        assertEquals(GameConfig.PLAYER_HEARTS, kid2.hearts)
        assertNull(sim.snapshot(), "a two-player game isn't saved")

        val x0 = caleb.x
        val x1 = kid2.x
        repeat(120) { sim.step(listOf(steer(EAST), steer(WEST))) }
        assertTrue(caleb.x > x0 + 50f, "the first kid went east")
        assertTrue(kid2.x < x1 - 50f, "the second kid went west")
    }

    @Test fun aOnePlayerGameHasNoSecondKid() {
        val sim = GameSim(seed = 3)
        assertNull(sim.together)
        assertEquals(listOf(sim.player), sim.players)
    }

    @Test fun theBiggerKidGobblesTheSmallerOneWhoComesBackFresh() {
        val sim = emptyTwoPlayerSim()
        val big = sim.player(1000f, 1000f, EAST, 600f)
        val small = sim.player(1250f, 1000f, WEST, 150f)
        small.grow(100f) // best length 250 this game
        small.snakesEaten = 2
        small.hearts = 1
        small.givePower(PowerType.SPEED)
        sim.stepUntil { !small.alive }
        val state = sim.together!!.kids[1]
        assertEquals(1, big.snakesEaten, "the bigger kid ate the smaller one")
        assertFalse(sim.gameOver, "a gobbled kid doesn't end the game")
        assertTrue(state.backIn > 2.9f)
        assertEquals(big.name, state.gobbledBy?.name)
        assertEquals(1, state.timesGobbled)

        // Back after three seconds: same snake, fresh and small, blinking safe, nowhere near the big one.
        sim.steps((GameConfig.COME_BACK_SECONDS * 60).toInt() - 5)
        assertFalse(small.alive)
        sim.steps(10)
        assertTrue(small.alive)
        assertEquals(0f, state.backIn)
        assertEquals(GameConfig.PLAYER_START_LENGTH, small.length)
        assertEquals(GameConfig.PLAYER_HEARTS, small.hearts)
        assertTrue(small.invulnerable > 0f)
        assertFalse(small.hasPower(PowerType.SPEED))
        assertTrue(dist(small.x, small.y, big.x, big.y) > 600f, "came back somewhere quiet")
        // The game's score and snakes eaten carry on.
        assertEquals(GameConfig.scoreFor(250f), small.score)
        assertEquals(2, small.snakesEaten)
        assertEquals(1, sim.world.snakes.count { it === small })
        assertSame(small, sim.players[1])
    }

    @Test fun aGobbledKidsEventSaysWhoItWas() {
        val sim = emptyTwoPlayerSim()
        sim.player(1000f, 1000f, EAST, 600f)
        val small = sim.player(1250f, 1000f, WEST, 150f)
        small.hearts = 1
        var eaten: GameEvent.PlayerEaten? = null
        sim.stepUntil {
            eaten = eaten ?: sim.events.filterIsInstance<GameEvent.PlayerEaten>().firstOrNull()
            eaten != null
        }
        assertEquals(small.id, eaten?.snakeId)
    }

    @Test fun theGameEndsWhenTheSharedClockRunsOut() {
        val sim = emptyTwoPlayerSim(seconds = 2f)
        sim.player(800f, 1000f, NORTH, 150f)
        sim.player(1200f, 1000f, NORTH, 150f)
        val clock = assertNotNull(sim.together)
        sim.steps(115)
        assertFalse(sim.gameOver)
        assertTrue(clock.timeLeft in 0.01f..0.1f)
        sim.steps(10)
        assertTrue(sim.gameOver)
        assertEquals(0f, clock.timeLeft)
        // The world keeps going behind the end screen, but the clock and comebacks stop.
        sim.steps(10)
        assertEquals(0f, clock.timeLeft)
    }

    @Test fun theCrownGoesToTheBiggerKidAndEachHasTheirOwnDangers() {
        val sim = emptyTwoPlayerSim()
        val bigKid = sim.player(1000f, 1000f, EAST, 400f)
        val smallKid = sim.player(1000f, 2000f, EAST, 150f)
        // A medium snake heading at the small kid, from far away: bigger than one kid, smaller than the other.
        sim.ai(1000f, 2700f, NORTH, 250f)
        sim.step()
        assertTrue(sim.isOnTop(bigKid))
        assertFalse(sim.isOnTop(smallKid))
        assertEquals(1, sim.together!!.kids.count { it.onTop })
        assertEquals(1, sim.dangers(smallKid).size)
        assertTrue(sim.dangers(bigKid).isEmpty())
    }

    @Test fun eitherKidCanPickUpAPowerUp() {
        val sim = emptyTwoPlayerSim()
        sim.player(800f, 1000f, NORTH, 150f)
        val kid2 = sim.player(1600f, 1000f, NORTH, 150f)
        sim.world.powerUps.add(PowerUp(PowerType.ARMOR, kid2.x, kid2.y - 10f))
        sim.step()
        assertTrue(kid2.hasPower(PowerType.ARMOR))
        assertTrue(sim.world.powerUps.isEmpty())
    }

    @Test fun newSnakesAppearOffBothKidsScreens() {
        val sim = GameSim(seed = 11, secondPlayer = mya)
        val seen = HashSet<Int>()
        repeat(1200) {
            for (s in sim.world.snakes) {
                if (s.isPlayer || !seen.add(s.id)) continue
                for (kid in sim.players.filter { it.alive }) {
                    val d = dist(s.x, s.y, kid.x, kid.y)
                    assertTrue(d >= GameConfig.offscreenDistance(kid.radius) - 1f, "${s.name} appeared on ${kid.name}'s screen ($d)")
                }
            }
            sim.step(listOf(steer(WEST), steer(EAST)))
        }
        assertTrue(seen.size > GameConfig.AI_TARGET_COUNT, "some snakes came and went")
        assertNotNull(sim.together)
    }
}
