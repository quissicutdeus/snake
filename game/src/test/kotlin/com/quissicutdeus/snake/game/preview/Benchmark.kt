package com.quissicutdeus.snake.game.preview

import com.quissicutdeus.snake.game.GameConfig
import com.quissicutdeus.snake.game.render.Camera
import com.quissicutdeus.snake.game.render.Canvas2D
import com.quissicutdeus.snake.game.render.Fx
import com.quissicutdeus.snake.game.render.Path2D
import com.quissicutdeus.snake.game.render.SceneRenderer
import com.quissicutdeus.snake.game.render.TextAlign
import com.quissicutdeus.snake.game.sim.Ai
import com.quissicutdeus.snake.game.sim.AiBrain
import com.quissicutdeus.snake.game.sim.GameSim
import com.quissicutdeus.snake.game.sim.SteerInput
import kotlin.random.Random

/**
 * Times the simulation step and the renderer's own work on the JVM:
 *   ./gradlew :game:benchmark
 *
 * Each game has a bot "player" (the AI brain), which grows big and so stresses
 * the long-snake paths. Rendering goes to a canvas that only counts calls, so
 * the frame time is the game's drawing code, not any rasteriser; on a phone
 * the real cost of a frame is mostly in the draw calls it counts.
 *
 * The checksum is a fingerprint of the final world. An optimisation that is
 * meant to change nothing must leave it identical.
 */
fun main() {
    // Warm up the JIT on a game that isn't measured.
    playGame(seed = 1000L, steps = STEPS)
    val results = SEEDS.map { playGame(it, STEPS) }
    println("Benchmark: ${SEEDS.size} games x $STEPS steps (${STEPS / STEPS_PER_SECOND} s of play each)")
    for (r in results) {
        println(
            "  seed %d: step %6.1f us (median %6.1f, p99 %6.1f)  frame %6.1f us  %5d draw calls/frame  player len %5.0f  checksum %08x"
                .format(
                    r.seed,
                    r.stepMeanUs,
                    r.stepMedianUs,
                    r.stepP99Us,
                    r.frameMeanUs,
                    r.drawCallsPerFrame,
                    r.playerLength,
                    r.checksum,
                ),
        )
    }
    println(
        "  mean:    step %6.1f us  frame %6.1f us  combined checksum %08x".format(
            results.map { it.stepMeanUs }.average(),
            results.map { it.frameMeanUs }.average(),
            results.fold(0) { acc, r -> acc * 31 + r.checksum },
        ),
    )
}

private const val STEPS_PER_SECOND = 60
private const val STEPS = STEPS_PER_SECOND * 120
private val SEEDS = listOf(1L, 2L, 3L, 4L, 5L)

private class Result(
    val seed: Long,
    val stepMeanUs: Double,
    val stepMedianUs: Double,
    val stepP99Us: Double,
    val frameMeanUs: Double,
    val drawCallsPerFrame: Long,
    val playerLength: Float,
    val checksum: Int,
)

private fun playGame(
    seed: Long,
    steps: Int,
): Result {
    val sim = GameSim(seed = seed)
    val p = sim.player!!
    p.brain = AiBrain(Random(seed))
    val input = SteerInput()
    val canvas = CountingCanvas(1080f, 2340f)
    val camera = Camera()
    val fx = Fx()
    val stepNanos = LongArray(steps)
    var frameNanos = 0L
    for (i in 0 until steps) {
        // The bot player keeps playing; after game over the world still runs.
        if (p.alive) input.set(Ai.desiredHeading(sim, p, GameConfig.STEP)) else input.release()
        val t0 = System.nanoTime()
        sim.step(input)
        val t1 = System.nanoTime()
        stepNanos[i] = t1 - t0
        fx.onEvents(sim, sim.events)
        sim.events.clear()
        fx.update(GameConfig.STEP)
        camera.followSnake(p, 1f, canvas.width, canvas.height, GameConfig.STEP)
        val t2 = System.nanoTime()
        SceneRenderer.draw(canvas, sim, camera, fx, i * GameConfig.STEP)
        frameNanos += System.nanoTime() - t2
    }
    stepNanos.sort()
    return Result(
        seed = seed,
        stepMeanUs = stepNanos.average() / 1000.0,
        stepMedianUs = stepNanos[steps / 2] / 1000.0,
        stepP99Us = stepNanos[steps * 99 / 100] / 1000.0,
        frameMeanUs = frameNanos / steps / 1000.0,
        drawCallsPerFrame = canvas.calls / steps,
        playerLength = p.length,
        checksum = checksum(sim),
    )
}

/** Fingerprint of everything the simulation decides. */
private fun checksum(sim: GameSim): Int {
    var h = sim.world.snakes.size

    fun mix(f: Float) {
        h = h * 31 + f.toRawBits()
    }
    for (s in sim.world.snakes) {
        mix(s.x)
        mix(s.y)
        mix(s.heading)
        mix(s.length)
        h = h * 31 + s.segCount
        h = h * 31 + s.hearts
    }
    for (f in sim.world.pellets) {
        mix(f.x)
        mix(f.y)
    }
    for (t in sim.world.tailPieces) {
        mix(t.x)
        mix(t.y)
    }
    return h
}

/** A [Canvas2D] that draws nothing and counts the calls that would draw. */
private class CountingCanvas(
    override val width: Float,
    override val height: Float,
) : Canvas2D {
    var calls = 0L

    override fun save() = Unit

    override fun restore() = Unit

    override fun translate(
        dx: Float,
        dy: Float,
    ) = Unit

    override fun scale(
        sx: Float,
        sy: Float,
    ) = Unit

    override fun rotate(radians: Float) = Unit

    override fun fillCircle(
        cx: Float,
        cy: Float,
        r: Float,
        color: Int,
    ) {
        calls++
    }

    override fun strokeCircle(
        cx: Float,
        cy: Float,
        r: Float,
        width: Float,
        color: Int,
    ) {
        calls++
    }

    override fun fillRect(
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        color: Int,
    ) {
        calls++
    }

    override fun fillRoundRect(
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        radius: Float,
        color: Int,
    ) {
        calls++
    }

    override fun line(
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float,
        width: Float,
        color: Int,
    ) {
        calls++
    }

    override fun fillPath(
        path: Path2D,
        color: Int,
    ) {
        calls++
    }

    override fun strokePath(
        path: Path2D,
        width: Float,
        color: Int,
    ) {
        calls++
    }

    override fun text(
        text: String,
        x: Float,
        y: Float,
        size: Float,
        color: Int,
        align: TextAlign,
        bold: Boolean,
    ) {
        calls++
    }

    override fun strokeText(
        text: String,
        x: Float,
        y: Float,
        size: Float,
        width: Float,
        color: Int,
        align: TextAlign,
    ) {
        calls++
    }

    // Roughly what a real font measures, so the HUD's name fitting does its usual work.
    override fun measureText(
        text: String,
        size: Float,
        bold: Boolean,
    ): Float = text.length * size * 0.55f
}
