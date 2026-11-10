package com.snakearena.game.preview

import com.snakearena.game.GameConfig
import com.snakearena.game.sim.Ai
import com.snakearena.game.sim.AiBrain
import com.snakearena.game.sim.GameEvent
import com.snakearena.game.sim.GameSim
import com.snakearena.game.sim.SteerInput
import kotlin.random.Random

/**
 * Plays a few games with a computer-controlled "player" (using the AI brain)
 * and prints how long it survived, how big it got and the size mix around it.
 * Handy after changing GameConfig:  ./gradlew :game:balanceReport
 */
fun main() {
    for (seed in 1L..6L) {
        val sim = GameSim(seed = seed)
        val p = sim.player!!
        p.brain = AiBrain(Random(seed))
        val input = SteerInput()
        var steps = 0
        var hurts = 0
        var cuts = 0
        var eats = 0
        val log = StringBuilder()
        while (steps < 60 * 300 && !sim.gameOver) {
            input.set(Ai.desiredHeading(sim, p, GameConfig.STEP))
            sim.step(input)
            for (e in sim.events) when (e) {
                is GameEvent.Hurt -> if (e.snakeId == p.id) hurts++
                is GameEvent.TailCut -> if (e.victimId == p.id) cuts++
                is GameEvent.AteSnake -> if (e.eaterId == p.id) eats++
                else -> Unit
            }
            sim.events.clear()
            steps++
            if (steps % (60 * 30) == 0) {
                val ratios = sim.world.snakes.filter { it.alive && !it.isPlayer }
                    .map { (it.length / p.length * 10).toInt() / 10f }.sorted()
                log.append("    ${steps / 60}s: score ${GameConfig.scoreFor(p.length)}, hearts ${p.hearts}, AI size / yours $ratios\n")
            }
        }
        println("Game $seed: lasted ${steps / 60}s, best score ${p.score}, hurt $hurts×, lost tail $cuts×, ate $eats snakes")
        print(log)
    }
}
