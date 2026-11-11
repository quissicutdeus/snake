package com.quissicutdeus.snake.game.audio

import com.quissicutdeus.snake.game.model.Snake
import com.quissicutdeus.snake.game.sim.GameEvent

/**
 * Turns a frame's events into sounds, from the player's point of view: the
 * player's own big moments, and a pop for each food dot the player eats (rising
 * in pitch through a quick run of them). At most one big sound and one pop per
 * frame, and a sound doesn't repeat within [REPEAT_SECONDS].
 */
class SoundCues {
    private val lastPlayed = FloatArray(Sound.entries.size) { Float.MAX_VALUE }
    private var sinceWall = Float.MAX_VALUE
    private var sincePellet = Float.MAX_VALUE
    private var pelletRun = 0

    /** Sounds to start this frame (empty most frames). [dt] is the frame time in seconds. */
    fun onEvents(
        events: List<GameEvent>,
        player: Snake,
        dt: Float,
    ): List<Sound> {
        for (i in lastPlayed.indices) lastPlayed[i] += dt
        val wasOnWall = sinceWall + dt < WALL_GAP_SECONDS
        sinceWall += dt
        sincePellet += dt

        val big = biggest(events, player.id, wasOnWall)
        val ateFood = player.alive && events.any { it is GameEvent.AteFood && isAtHead(it, player) }

        val out = ArrayList<Sound>(2)
        if (big != null && lastPlayed[big.ordinal] >= REPEAT_SECONDS) out += big
        if (ateFood && sincePellet >= PELLET_GAP_SECONDS) out += pellet()
        for (s in out) lastPlayed[s.ordinal] = 0f
        return out
    }

    /** The most important of the player's big moments this frame. */
    private fun biggest(
        events: List<GameEvent>,
        pid: Int,
        wasOnWall: Boolean,
    ): Sound? {
        var big: Sound? = null
        for (e in events) {
            val s = bigSound(e, pid)
            if (s == Sound.WALL) sinceWall = 0f
            val wanted = s != null && !(s == Sound.WALL && wasOnWall)
            if (wanted && (big == null || PRIORITY.indexOf(s) < PRIORITY.indexOf(big))) big = s
        }
        return big
    }

    private fun pellet(): Sound {
        pelletRun = if (sincePellet < PELLET_RUN_SECONDS) pelletRun + 1 else 0
        sincePellet = 0f
        return PELLETS[minOf(pelletRun / PELLETS_PER_STEP, PELLETS.lastIndex)]
    }

    private fun isAtHead(
        e: GameEvent.AteFood,
        p: Snake,
    ): Boolean {
        val dx = e.x - p.x
        val dy = e.y - p.y
        val reach = p.radius + HEAD_REACH
        return dx * dx + dy * dy <= reach * reach
    }

    private fun bigSound(
        e: GameEvent,
        pid: Int,
    ): Sound? =
        when (e) {
            is GameEvent.PlayerEaten -> Sound.GOBBLED
            is GameEvent.BecameBiggest -> Sound.FANFARE
            is GameEvent.PowerUpTaken -> Sound.POWER_UP
            is GameEvent.AteSnake -> Sound.CHOMP.takeIf { e.eaterId == pid }
            is GameEvent.TailCut -> tailCut(e, pid)
            is GameEvent.Hurt -> Sound.OUCH.takeIf { e.snakeId == pid }
            is GameEvent.Bounced -> Sound.BOING.takeIf { e.snakeId == pid }
            is GameEvent.Blocked -> Sound.CLANG.takeIf { e.snakeId == pid }
            is GameEvent.HitWall -> Sound.WALL.takeIf { e.snakeId == pid }
            is GameEvent.AteFood -> null
        }

    private fun tailCut(
        e: GameEvent.TailCut,
        pid: Int,
    ): Sound? =
        when (pid) {
            e.victimId -> Sound.OUCH
            e.attackerId -> Sound.SNIP
            else -> null
        }

    companion object {
        /** The same sound waits this long before it can play again. */
        const val REPEAT_SECONDS = 0.1f

        /** At most one food pop this often. */
        const val PELLET_GAP_SECONDS = 0.06f

        /** Pops closer together than this count as one run, which rises in pitch. */
        const val PELLET_RUN_SECONDS = 0.35f

        /** Pops per pitch step within a run. */
        const val PELLETS_PER_STEP = 3

        /** Away from the fence this long and touching it sounds again. */
        const val WALL_GAP_SECONDS = 0.25f

        /** Food eaten within this distance of the player's head (beyond its radius) is the player's. */
        private const val HEAD_REACH = 40f

        private val PELLETS = listOf(Sound.PELLET_LOW, Sound.PELLET_MID, Sound.PELLET_HIGH)

        /** Most important first: when several happen in one frame, the first wins. */
        private val PRIORITY =
            listOf(
                Sound.GOBBLED,
                Sound.OUCH,
                Sound.FANFARE,
                Sound.CHOMP,
                Sound.POWER_UP,
                Sound.CLANG,
                Sound.SNIP,
                Sound.BOING,
                Sound.WALL,
            )
    }
}
