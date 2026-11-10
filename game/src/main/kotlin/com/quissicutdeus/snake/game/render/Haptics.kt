package com.quissicutdeus.snake.game.render

import com.quissicutdeus.snake.game.sim.GameEvent

/** Kinds of phone buzz, weakest first. The app maps each one to a system haptic. */
enum class Buzz {
    /** Bumped into the fence. */
    TAP,

    /** BOING, SNIP or CLANG. */
    THUD,

    /** CHOMP (ate a snake) or grabbed a power-up. */
    CHEER,

    /** OUCH, or lost the tail. */
    OUCH,

    /** The player got gobbled. */
    GOBBLED,
}

/**
 * Picks at most one [Buzz] per frame from the player's own moments, so the
 * phone never rattles nonstop: the strongest event wins, the same or a weaker
 * buzz waits out [COOLDOWN_SECONDS], and leaning on the fence buzzes once.
 */
class Haptics {
    private var cooldown = 0f
    private var last: Buzz? = null
    private var sinceWall = Float.MAX_VALUE

    /** This frame's buzz, or null for none. [dt] is the frame time in seconds. */
    fun onEvents(
        events: List<GameEvent>,
        playerId: Int,
        dt: Float,
    ): Buzz? {
        cooldown = (cooldown - dt).coerceAtLeast(0f)
        // Still leaning on the fence if it was touched recently (frames on a
        // 120 Hz screen often run no game step, so they carry no events).
        val wasOnWall = sinceWall + dt < WALL_GAP_SECONDS
        sinceWall += dt
        var best: Buzz? = null
        for (e in events) {
            val b = buzzFor(e, playerId)
            if (b == Buzz.TAP) sinceWall = 0f
            val wanted = b != null && !(b == Buzz.TAP && wasOnWall)
            if (wanted && (best == null || b > best)) best = b
        }
        if (best == null || isRepeat(best)) return null
        cooldown = COOLDOWN_SECONDS
        last = best
        return best
    }

    /** The same or a weaker buzz than the last one, still within its cooldown. */
    private fun isRepeat(b: Buzz): Boolean {
        val prev = last ?: return false
        return cooldown > 0f && b <= prev
    }

    private fun buzzFor(
        e: GameEvent,
        pid: Int,
    ): Buzz? =
        when (e) {
            is GameEvent.PlayerEaten -> Buzz.GOBBLED

            is GameEvent.PowerUpTaken -> Buzz.CHEER

            is GameEvent.AteSnake -> Buzz.CHEER.takeIf { e.eaterId == pid }

            is GameEvent.TailCut -> tailCut(e, pid)

            is GameEvent.Hurt -> Buzz.OUCH.takeIf { e.snakeId == pid }

            is GameEvent.Bounced -> Buzz.THUD.takeIf { e.snakeId == pid }

            is GameEvent.Blocked -> Buzz.THUD.takeIf { e.snakeId == pid }

            is GameEvent.HitWall -> Buzz.TAP.takeIf { e.snakeId == pid }

            // Pellets are eaten constantly; buzzing for each would never stop.
            is GameEvent.AteFood -> null
        }

    private fun tailCut(
        e: GameEvent.TailCut,
        pid: Int,
    ): Buzz? =
        when (pid) {
            e.victimId -> Buzz.OUCH
            e.attackerId -> Buzz.THUD
            else -> null
        }

    companion object {
        /** After a buzz, the same or a weaker one waits this long. */
        const val COOLDOWN_SECONDS = 0.15f

        /** Away from the fence this long and touching it buzzes again. */
        const val WALL_GAP_SECONDS = 0.25f
    }
}
