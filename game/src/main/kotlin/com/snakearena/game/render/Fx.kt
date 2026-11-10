package com.snakearena.game.render

import com.snakearena.game.model.TWO_PI
import com.snakearena.game.sim.GameEvent
import com.snakearena.game.sim.GameSim
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

/**
 * Juice: pop particles, floating comic words and screen shake, driven by
 * [GameEvent]s. Purely visual; never touches the simulation.
 */
class Fx(seed: Long = 1) {
    private val rng = Random(seed)

    class Particle(
        var x: Float, var y: Float, var vx: Float, var vy: Float,
        var life: Float, val maxLife: Float, val size: Float, val color: Int,
    )

    class Word(val text: String, var x: Float, var y: Float, var life: Float, val maxLife: Float, val color: Int, val size: Float)

    val particles = ArrayList<Particle>()
    val words = ArrayList<Word>()

    /** Current shake offset in screen pixels. */
    var shakeX = 0f
        private set
    var shakeY = 0f
        private set
    private var shake = 0f

    /** Seconds of "ouch" red flash left (player hurt). */
    var hurtFlash = 0f
        private set

    /** Feeds this frame's events. Effects far from the player are skipped. */
    fun onEvents(sim: GameSim, events: List<GameEvent>) {
        val p = sim.player
        val pid = p?.id ?: -1
        for (e in events) {
            if (p != null && p.alive) {
                val dx = e.x - p.x
                val dy = e.y - p.y
                if (dx * dx + dy * dy > 1600f * 1600f) continue
            }
            when (e) {
                is GameEvent.AteFood -> burst(e.x, e.y, e.color, count = 4, speed = 90f, size = 3.5f, life = 0.35f)
                is GameEvent.AteSnake -> {
                    burst(e.x, e.y, e.color, count = 26, speed = 260f, size = 7f, life = 0.8f)
                    if (e.eaterId == pid) {
                        word("CHOMP!", e.x, e.y, 0xFFFFD93D.toInt(), 34f)
                        addShake(10f)
                    }
                }
                is GameEvent.TailCut -> {
                    burst(e.x, e.y, e.color, count = 14, speed = 180f, size = 5f, life = 0.6f)
                    if (e.attackerId == pid) word("SNIP!", e.x, e.y, 0xFFFFFFFF.toInt(), 28f)
                    if (e.victimId == pid) {
                        word("My tail!", e.x, e.y, 0xFFFFAB40.toInt(), 26f)
                        addShake(8f)
                    }
                }
                is GameEvent.Hurt -> if (e.snakeId == pid) {
                    burst(e.x, e.y, 0xFFFFEB3B.toInt(), count = 10, speed = 140f, size = 6f, life = 0.6f)
                    word("OUCH!", e.x, e.y, 0xFFFF5252.toInt(), 30f)
                    addShake(16f)
                    hurtFlash = 0.35f
                }
                is GameEvent.Bounced -> {
                    burst(e.x, e.y, 0xFFFFFFFF.toInt(), count = 5, speed = 110f, size = 4f, life = 0.3f)
                    if (e.snakeId == pid) {
                        addShake(6f)
                        word("BOING!", e.x, e.y, 0xFFB388FF.toInt(), 24f)
                    }
                }
                is GameEvent.HitWall -> if (e.snakeId == pid) addShake(3f)
                is GameEvent.PlayerEaten -> {
                    burst(e.x, e.y, 0xFFFFFFFF.toInt(), count = 40, speed = 320f, size = 8f, life = 1.2f)
                    word("GULP!", e.x, e.y, 0xFFFF5252.toInt(), 44f)
                    addShake(24f)
                }
            }
        }
    }

    fun update(dt: Float) {
        val it = particles.iterator()
        while (it.hasNext()) {
            val q = it.next()
            q.life -= dt
            if (q.life <= 0f) { it.remove(); continue }
            q.x += q.vx * dt
            q.y += q.vy * dt
            q.vx *= 0.9f
            q.vy *= 0.9f
        }
        val w = words.iterator()
        while (w.hasNext()) {
            val q = w.next()
            q.life -= dt
            if (q.life <= 0f) { w.remove(); continue }
            q.y -= 60f * dt
        }
        shake = max(0f, shake - dt * 60f)
        shakeX = if (shake > 0f) (rng.nextFloat() - 0.5f) * 2f * shake else 0f
        shakeY = if (shake > 0f) (rng.nextFloat() - 0.5f) * 2f * shake else 0f
        hurtFlash = max(0f, hurtFlash - dt)
    }

    fun clear() {
        particles.clear()
        words.clear()
        shake = 0f
        shakeX = 0f
        shakeY = 0f
        hurtFlash = 0f
    }

    private fun addShake(amount: Float) {
        shake = max(shake, amount)
    }

    private fun burst(x: Float, y: Float, color: Int, count: Int, speed: Float, size: Float, life: Float) {
        if (particles.size > 600) return
        repeat(count) {
            val a = rng.nextFloat() * TWO_PI
            val v = speed * (0.4f + rng.nextFloat() * 0.8f)
            val l = life * (0.6f + rng.nextFloat() * 0.6f)
            particles.add(Particle(x, y, cos(a) * v, sin(a) * v, l, l, size * (0.6f + rng.nextFloat() * 0.8f), color))
        }
    }

    private fun word(text: String, x: Float, y: Float, color: Int, size: Float) {
        if (words.size > 6) words.removeAt(0)
        words.add(Word(text, x, y - 30f, 0.9f, 0.9f, color, size))
    }
}
