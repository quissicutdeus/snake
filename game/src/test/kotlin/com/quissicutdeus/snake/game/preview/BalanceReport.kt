package com.quissicutdeus.snake.game.preview

import com.quissicutdeus.snake.game.GameConfig
import com.quissicutdeus.snake.game.sim.Ai
import com.quissicutdeus.snake.game.sim.AiBrain
import com.quissicutdeus.snake.game.sim.GameEvent
import com.quissicutdeus.snake.game.sim.GameSim
import com.quissicutdeus.snake.game.sim.SteerInput
import kotlin.random.Random

/**
 * Plays a few games with a computer-controlled "player" (using the AI brain)
 * and prints how long it survived, how big it got and the size mix around it.
 * Handy after changing GameConfig:  ./gradlew :game:balanceReport
 */
fun main() {
    for (seed in 1L..6L) playOneGame(seed)
}

/** Counts of what happened to the bot player during one game. */
private class Tally {
    var hurts = 0
    var cuts = 0
    var eats = 0

    fun count(
        e: GameEvent,
        playerId: Int,
    ) {
        when (e) {
            is GameEvent.Hurt -> {
                if (e.snakeId == playerId) hurts++
            }

            is GameEvent.TailCut -> {
                if (e.victimId == playerId) cuts++
            }

            is GameEvent.AteSnake -> {
                if (e.eaterId == playerId) eats++
            }

            else -> {}
        }
    }
}

private fun playOneGame(seed: Long) {
    val sim = GameSim(seed = seed)
    val p = sim.player!!
    p.brain = AiBrain(Random(seed))
    val input = SteerInput()
    val tally = Tally()
    val log = StringBuilder()
    var steps = 0
    while (steps < 60 * 300 && !sim.gameOver) {
        input.set(Ai.desiredHeading(sim, p, GameConfig.STEP))
        sim.step(input)
        sim.events.forEach { tally.count(it, p.id) }
        sim.events.clear()
        steps++
        if (steps % (60 * 30) == 0) {
            val ratios =
                sim.world.snakes
                    .filter { it.alive && !it.isPlayer }
                    .map { (it.length / p.length * 10).toInt() / 10f }
                    .sorted()
            log.append("    ${steps / 60}s: score ${GameConfig.scoreFor(p.length)}, hearts ${p.hearts}, AI size / yours $ratios\n")
        }
    }
    println(
        "Game $seed: lasted ${steps / 60}s, best score ${p.score}, hurt ${tally.hurts} times, " +
            "lost tail ${tally.cuts} times, ate ${tally.eats} snakes",
    )
    print(log)
}
