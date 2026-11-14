package com.quissicutdeus.sniddle.game

import com.quissicutdeus.sniddle.game.model.PowerType
import com.quissicutdeus.sniddle.game.model.SnailStyle
import com.quissicutdeus.sniddle.game.sim.GameEvent
import com.quissicutdeus.sniddle.game.sim.GameSim
import com.quissicutdeus.sniddle.game.sim.SecondPlayer
import com.quissicutdeus.sniddle.game.sim.SimOptions
import com.quissicutdeus.sniddle.game.sim.SteerInput
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Locks in the simulation's exact behaviour: three fixed-seed games are hashed
 * at a few checkpoints and must match the hashes recorded below, bit for bit.
 * A refactor meant to change nothing (moving code, tightening setters, moving
 * AI state around) must leave every hash as it is; the first checkpoint that
 * differs says roughly when the games went apart.
 *
 * Only what can be seen is hashed: the snails in world order, the food, the
 * power-ups, the players' powers, game over and every event. Not the AI
 * brains or anything else internal, so those can change freely.
 *
 * JVM only: kotlin.math sin/cos are not guaranteed to round the same on iOS.
 *
 * Regenerating, when behaviour changes on purpose: run
 *   ./gradlew :game:jvmTest --tests '*GoldenHashTest*' --rerun
 * and each failing scenario prints its new hashes as Kotlin to paste over the
 * old ones. Say in the commit why the game now plays differently.
 */
class GoldenHashTest {
    @Test fun onePlayerSteeringAtRandom() {
        val sim = GameSim(seed = 4242L)
        val input = SteerInput()
        val steer = RandomSteering(Random(7))
        check("ONE_PLAYER", ONE_PLAYER, play(sim) { i -> sim.step(steer.next(i, input)) })
    }

    @Test fun twoPlayersSteeringAtRandom() {
        val sim = GameSim(seed = 777L, secondPlayer = SecondPlayer("Two", SnailStyle.DEFAULT))
        val inputs = listOf(SteerInput(), SteerInput())
        val steering = listOf(RandomSteering(Random(11)), RandomSteering(Random(13)))
        check(
            "TWO_PLAYERS",
            TWO_PLAYERS,
            play(sim) { i ->
                steering.forEachIndexed { k, s -> s.next(i, inputs[k]) }
                sim.step(inputs)
            },
        )
    }

    @Test fun computerSnailsOnly() {
        val sim = GameSim(seed = 2024L, options = SimOptions(spawnPlayer = false))
        check("AI_ONLY", AI_ONLY, play(sim) { sim.step() })
    }

    /** Turns a little every half second, and now and then lets go. */
    private class RandomSteering(
        private val rng: Random,
    ) {
        private var heading = 0f

        fun next(
            step: Int,
            input: SteerInput,
        ): SteerInput {
            if (step % STEER_EVERY == 0) {
                heading += (rng.nextFloat() - 0.5f) * 3f
                if (rng.nextInt(5) == 0) input.release() else input.set(heading)
            }
            return input
        }
    }

    /** Runs [step] up to the last checkpoint and returns the hash at each one. */
    private fun play(
        sim: GameSim,
        step: (Int) -> Unit,
    ): List<Long> {
        val hashes = ArrayList<Long>()
        // Events from every step so far, folded in as they happen (step() never clears them).
        val events = Fnv()
        for (i in 1..CHECKPOINTS.last()) {
            step(i - 1)
            for (e in sim.events) events.event(e)
            sim.events.clear()
            if (i in CHECKPOINTS) hashes.add(Fnv().apply { state(sim) }.add(events.hash).hash)
        }
        return hashes
    }

    private fun check(
        name: String,
        expected: List<String>,
        actual: List<Long>,
    ) {
        assertEquals(
            expected,
            actual.map(::hex),
            "$name no longer plays the same (checkpoints at steps $CHECKPOINTS). " +
                "If that is on purpose, paste:\n" +
                "        val $name = listOf(${actual.joinToString { "\"${hex(it)}\"" }})\n",
        )
    }

    private fun hex(h: Long) = h.toULong().toString(16).padStart(16, '0')

    /** 64-bit FNV-1a, a word at a time: simple, and the same on every JVM. */
    private class Fnv {
        var hash = -0x340d631b7bdddcdbL // FNV offset basis 0xcbf29ce484222325
            private set

        fun add(v: Long) = apply { hash = (hash xor v) * 0x100000001b3L }

        fun add(v: Int) = add(v.toLong())

        fun add(v: Float) = add(v.toRawBits())

        fun add(v: Boolean) = add(if (v) 1 else 0)

        fun state(sim: GameSim) {
            val w = sim.world
            add(w.snails.size)
            for (s in w.snails) {
                add(s.id)
                    .add(s.alive)
                    .add(s.x)
                    .add(s.y)
                    .add(s.heading)
                    .add(s.length)
                    .add(s.bestLength)
                add(s.hearts).add(s.snailsEaten).add(s.invulnerable).add(s.segCount)
                // Only the live part: the arrays grow by doubling and keep stale values past segCount.
                for (k in 0 until s.segCount) add(s.segX[k]).add(s.segY[k]).add(s.segR[k]).add(s.segD[k])
            }
            add(w.pellets.size)
            for (p in w.pellets) add(p.x).add(p.y).add(p.value)
            add(w.tailPieces.size)
            for (t in w.tailPieces) add(t.x).add(t.y).add(t.value)
            add(w.powerUps.size)
            for (u in w.powerUps) add(u.type.ordinal).add(u.x).add(u.y)
            add(sim.players.size)
            for (p in sim.players) for (type in PowerType.entries) add(p.powerLeft(type))
            add(sim.gameOver)
        }

        /** Each kind of event gets its own tag; never hashCode(), which for an enum differs between runs. */
        fun event(e: GameEvent) {
            when (e) {
                is GameEvent.AteFood -> add(1).add(e.color)
                is GameEvent.AteSnail -> add(2).add(e.eaterId).add(e.color)
                is GameEvent.TailCut -> add(3).add(e.attackerId).add(e.victimId).add(e.color)
                is GameEvent.Hurt -> add(4).add(e.snailId)
                is GameEvent.Bounced -> add(5).add(e.snailId)
                is GameEvent.HitWall -> add(6).add(e.snailId)
                is GameEvent.PowerUpTaken -> add(7).add(e.type.ordinal)
                is GameEvent.Blocked -> add(8).add(e.snailId)
                is GameEvent.BecameBiggest -> add(9).add(e.snailId)
                is GameEvent.PlayerEaten -> add(10).add(e.snailId)
            }
            add(e.x).add(e.y)
        }
    }

    private companion object {
        const val STEER_EVERY = 30

        /**
         * Steps (at 60 a second) at which the games are hashed: 10 s, 30 s, 1 min, 2 min and 3 min 10 s in.
         * Long enough that the one player gets eaten (game over), a kid in the two-player game is
         * gobbled and comes back, and the two-player clock (3 min) runs out.
         */
        val CHECKPOINTS = listOf(600, 1800, 3600, 7200, 11400)

        val ONE_PLAYER = listOf("537e53722741c54d", "3e6b43f2e9def17f", "6b572d5181842942", "3e0dc9f3b10ccd0e", "6927a1496dec999e")
        val TWO_PLAYERS = listOf("16159525bd29c933", "83e4efbccd1320ea", "a010502dbd42d62a", "97537854c63c6c43", "7a440cfdddc663db")
        val AI_ONLY = listOf("7139597a67fe5326", "0886dbaadf20c2d5", "7b0ee1c2a6d1b353", "9b4d8ed3f98780cc", "3aed38a9476292b0")
    }
}
