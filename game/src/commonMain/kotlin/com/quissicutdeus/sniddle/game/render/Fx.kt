package com.quissicutdeus.sniddle.game.render

import com.quissicutdeus.sniddle.game.model.PI_F
import com.quissicutdeus.sniddle.game.model.PowerType
import com.quissicutdeus.sniddle.game.model.Snail
import com.quissicutdeus.sniddle.game.model.TWO_PI
import com.quissicutdeus.sniddle.game.model.dist2
import com.quissicutdeus.sniddle.game.sim.GameEvent
import com.quissicutdeus.sniddle.game.sim.GameSim
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Juice: pop particles, floating comic words and screen shake, driven by
 * [GameEvent]s. Purely visual; never touches the simulation.
 */
class Fx(
    seed: Long = 1,
) {
    private val rng = Random(seed)

    class Particle(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var life: Float,
        val maxLife: Float,
        val size: Float,
        val color: Int,
    )

    class Word(
        val text: String,
        val x: Float,
        var y: Float,
        var life: Float,
        val maxLife: Float,
        val color: Int,
        val size: Float,
    )

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

    /**
     * Feeds this frame's events. Effects far from the player are skipped. With
     * two kids, each half of the screen has its own [Fx] with its kid as [me]:
     * shakes and the red flash are only for that kid's own moments.
     */
    fun onEvents(
        sim: GameSim,
        events: List<GameEvent>,
        me: Snail? = sim.player,
    ) {
        speedTrail(me)
        val p = me
        val pid = p?.id ?: -1
        for (e in events) {
            if (p != null && p.alive && dist2(e.x, e.y, p.x, p.y) > FAR_AWAY * FAR_AWAY) continue
            when (e) {
                is GameEvent.AteFood -> burst(e.x, e.y, e.color, count = 4, speed = 90f, size = 3.5f, life = 0.35f)
                is GameEvent.AteSnail -> ateSnail(e, pid)
                is GameEvent.TailCut -> tailCut(e, pid)
                is GameEvent.Hurt -> if (e.snailId == pid) playerHurt(e)
                is GameEvent.Bounced -> bounced(e, pid)
                is GameEvent.HitWall -> if (e.snailId == pid) addShake(3f)
                is GameEvent.PlayerEaten -> playerEaten(e, pid)
                is GameEvent.PowerUpTaken -> powerUpTaken(e)
                is GameEvent.Blocked -> if (e.snailId == pid) blocked(e)
                is GameEvent.BecameBiggest -> becameBiggest(e, pid)
            }
        }
    }

    private fun ateSnail(
        e: GameEvent.AteSnail,
        pid: Int,
    ) {
        burst(e.x, e.y, e.color, count = 26, speed = 260f, size = 7f, life = 0.8f)
        if (e.eaterId == pid) {
            word("CHOMP!", e.x, e.y, 0xFFFFD93D.toInt(), 34f)
            addShake(10f)
        }
    }

    private fun tailCut(
        e: GameEvent.TailCut,
        pid: Int,
    ) {
        burst(e.x, e.y, e.color, count = 14, speed = 180f, size = 5f, life = 0.6f)
        if (e.attackerId == pid) word("SNIP!", e.x, e.y, 0xFFFFFFFF.toInt(), 28f)
        if (e.victimId == pid) {
            word("My slime!", e.x, e.y, 0xFFFFAB40.toInt(), 26f)
            addShake(8f)
        }
    }

    private fun playerHurt(e: GameEvent.Hurt) {
        burst(e.x, e.y, 0xFFFFEB3B.toInt(), count = 10, speed = 140f, size = 6f, life = 0.6f)
        word("OUCH!", e.x, e.y, 0xFFFF5252.toInt(), 30f)
        addShake(16f)
        hurtFlash = HURT_FLASH_SECONDS
    }

    private fun bounced(
        e: GameEvent.Bounced,
        pid: Int,
    ) {
        burst(e.x, e.y, 0xFFFFFFFF.toInt(), count = 5, speed = 110f, size = 4f, life = 0.3f)
        if (e.snailId == pid) {
            addShake(6f)
            word("BOING!", e.x, e.y, 0xFFB388FF.toInt(), 24f)
        }
    }

    private fun powerUpTaken(e: GameEvent.PowerUpTaken) {
        val col = PowerUpArt.color(e.type)
        burst(e.x, e.y, col, count = 30, speed = 260f, size = 7f, life = 0.9f)
        word("${e.type.label.uppercase()}!", e.x, e.y, col, 30f)
        addShake(6f)
    }

    /** The player became the biggest snail: gold and white confetti and a cheer. */
    private fun becameBiggest(
        e: GameEvent.BecameBiggest,
        pid: Int,
    ) {
        burst(e.x, e.y, Colors.GOLD, count = 36, speed = 300f, size = 7f, life = 1f)
        burst(e.x, e.y, 0xFFFFFFFF.toInt(), count = 14, speed = 220f, size = 5f, life = 0.8f)
        word("BIGGEST SNAIL!", e.x, e.y, Colors.GOLD, 32f)
        if (e.snailId == pid) addShake(6f)
    }

    /** Super Armor stopped a hit on the player. */
    private fun blocked(e: GameEvent.Blocked) {
        burst(e.x, e.y, PowerUpArt.color(PowerType.ARMOR), count = 12, speed = 200f, size = 5f, life = 0.5f)
        word("CLANG!", e.x, e.y, 0xFFB3E5FF.toInt(), 28f)
        addShake(5f)
    }

    /** Super Speed leaves a trail of sparkles behind the player. */
    private fun speedTrail(me: Snail?) {
        val p = me?.takeIf { it.alive && it.hasPower(PowerType.SPEED) } ?: return
        if (p.segCount < 4 || particles.size > 600) return
        val i = min(p.segCount - 1, 4 + rng.nextInt(6))
        val back = p.heading + PI_F + (rng.nextFloat() - 0.5f)
        particles.add(
            Particle(
                p.segX[i] + (rng.nextFloat() - 0.5f) * p.radius,
                p.segY[i] + (rng.nextFloat() - 0.5f) * p.radius,
                cos(back) * 140f,
                sin(back) * 140f,
                0.4f,
                0.4f,
                3f + rng.nextFloat() * 3f,
                if (rng.nextBoolean()) Colors.WHITE else PowerUpArt.color(PowerType.SPEED),
            ),
        )
    }

    private fun playerEaten(
        e: GameEvent.PlayerEaten,
        pid: Int,
    ) {
        burst(e.x, e.y, 0xFFFFFFFF.toInt(), count = 40, speed = 320f, size = 8f, life = 1.2f)
        word("GULP!", e.x, e.y, 0xFFFF5252.toInt(), 44f)
        if (e.snailId == pid) addShake(24f)
    }

    fun update(dt: Float) {
        val it = particles.iterator()
        while (it.hasNext()) {
            val q = it.next()
            q.life -= dt
            if (q.life <= 0f) {
                it.remove()
                continue
            }
            q.x += q.vx * dt
            q.y += q.vy * dt
            q.vx *= 0.9f
            q.vy *= 0.9f
        }
        val w = words.iterator()
        while (w.hasNext()) {
            val q = w.next()
            q.life -= dt
            if (q.life <= 0f) {
                w.remove()
                continue
            }
            q.y -= 60f * dt
        }
        shake = max(0f, shake - dt * 60f)
        shakeX = if (shake > 0f) (rng.nextFloat() - 0.5f) * 2f * shake else 0f
        shakeY = if (shake > 0f) (rng.nextFloat() - 0.5f) * 2f * shake else 0f
        hurtFlash = max(0f, hurtFlash - dt)
    }

    private fun addShake(amount: Float) {
        shake = max(shake, amount)
    }

    private fun burst(
        x: Float,
        y: Float,
        color: Int,
        count: Int,
        speed: Float,
        size: Float,
        life: Float,
    ) {
        if (particles.size > 600) return
        repeat(count) {
            val a = rng.nextFloat() * TWO_PI
            val v = speed * (0.4f + rng.nextFloat() * 0.8f)
            val l = life * (0.6f + rng.nextFloat() * 0.6f)
            particles.add(Particle(x, y, cos(a) * v, sin(a) * v, l, l, size * (0.6f + rng.nextFloat() * 0.8f), color))
        }
    }

    private fun word(
        text: String,
        x: Float,
        y: Float,
        color: Int,
        size: Float,
    ) {
        if (words.size > 6) words.removeAt(0)
        words.add(Word(text, x, y - 30f, 0.9f, 0.9f, color, size))
    }

    companion object {
        /** How long the red "ouch" flash lasts. */
        const val HURT_FLASH_SECONDS = 0.35f

        /** Effects further than this from the player are skipped. */
        private const val FAR_AWAY = 1600f
    }
}
