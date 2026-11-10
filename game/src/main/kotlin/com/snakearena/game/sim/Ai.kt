package com.snakearena.game.sim

import com.snakearena.game.GameConfig
import com.snakearena.game.model.Pellet
import com.snakearena.game.model.Snake
import com.snakearena.game.model.TailPiece
import com.snakearena.game.model.dist
import com.snakearena.game.model.dist2
import com.snakearena.game.model.wrapAngle
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

enum class AiMode { WANDER, FOOD, CHASE, FLEE }

/** Per-snake AI memory. */
class AiBrain(rng: Random) {
    var mode = AiMode.WANDER
        internal set
    internal var thinkTimer = rng.nextFloat() * GameConfig.AI_THINK_INTERVAL
    internal var wanderHeading = Float.NaN
    internal var wanderTimer = 0f
    internal var targetSnakeId = -1
    internal var food: Any? = null
    internal var fleeHeading = 0f
    internal var fleeFromId = -1
    internal var fleeTimer = 0f
    internal var chaseTime = 0f
    internal var chaseRest = 0f

    /** Run away from [threat] for a while (after being bitten or bumped). */
    fun startFleeing(threat: Snake) {
        fleeFromId = threat.id
        fleeTimer = 1.5f
        mode = AiMode.FLEE
    }
}

/** Wander -> seek food -> chase smaller snakes -> flee bigger ones. Same rules as the player. */
object Ai {

    /** Returns the heading this AI snake wants this step. */
    fun desiredHeading(sim: GameSim, s: Snake, dt: Float): Float {
        val brain = s.brain ?: return Float.NaN
        brain.chaseRest = (brain.chaseRest - dt).coerceAtLeast(0f)
        brain.fleeTimer = (brain.fleeTimer - dt).coerceAtLeast(0f)
        brain.wanderTimer -= dt
        brain.thinkTimer -= dt
        if (brain.thinkTimer <= 0f) {
            brain.thinkTimer += GameConfig.AI_THINK_INTERVAL
            think(sim, s, brain)
        }

        var desired = when (brain.mode) {
            AiMode.FLEE -> brain.fleeHeading
            AiMode.CHASE -> chaseHeading(sim, s, brain, dt)
            AiMode.FOOD -> foodHeading(s, brain)
            AiMode.WANDER -> wanderHeading(sim, s, brain)
        }
        if (desired.isNaN()) desired = wanderHeading(sim, s, brain)
        desired = avoidBodies(sim, s, brain, desired)
        return avoidWalls(sim, s, desired)
    }

    private fun think(sim: GameSim, s: Snake, brain: AiBrain) {
        var fleeX = 0f
        var fleeY = 0f
        var threatened = false
        var prey: Snake? = null
        var preyD = Float.MAX_VALUE
        for (o in sim.world.snakes) {
            if (o === s || !o.alive) continue
            val d = dist(s.x, s.y, o.x, o.y).coerceAtLeast(1f)
            when (s.relationTo(o)) {
                SizeRelation.SMALLER -> if (d < GameConfig.AI_FLEE_RADIUS + o.radius * 4f && s.invulnerable <= 0f) {
                    val w = 1f / d
                    fleeX += (s.x - o.x) / d * w
                    fleeY += (s.y - o.y) / d * w
                    threatened = true
                }
                SizeRelation.BIGGER -> if (d < GameConfig.AI_CHASE_RADIUS && d < preyD && o.invulnerable <= 0f) {
                    prey = o
                    preyD = d
                }
                SizeRelation.EQUAL -> Unit
            }
        }

        if (threatened) {
            brain.mode = AiMode.FLEE
            brain.fleeHeading = atan2(fleeY, fleeX)
            brain.chaseTime = 0f
            return
        }
        if (brain.fleeTimer > 0f) {
            val t = sim.world.snakeById(brain.fleeFromId)
            if (t != null) brain.fleeHeading = atan2(s.y - t.y, s.x - t.x)
            brain.mode = AiMode.FLEE
            return
        }

        val p = prey
        if (p != null && brain.chaseRest <= 0f) {
            val alreadyChasing = brain.mode == AiMode.CHASE && brain.targetSnakeId == p.id
            if (alreadyChasing || sim.rng.nextFloat() < GameConfig.AI_AGGRESSION) {
                if (!alreadyChasing) brain.chaseTime = 0f
                brain.mode = AiMode.CHASE
                brain.targetSnakeId = p.id
                return
            }
            // Decided not to chase this time; don't reconsider for a moment.
            brain.chaseRest = 1.5f
        }
        if (brain.mode == AiMode.CHASE) brain.chaseTime = 0f

        val food = nearestFood(sim, s)
        if (food != null) {
            brain.mode = AiMode.FOOD
            brain.food = food
        } else {
            brain.mode = AiMode.WANDER
        }
    }

    private fun nearestFood(sim: GameSim, s: Snake): Any? {
        val maxD2 = GameConfig.AI_FOOD_RADIUS * GameConfig.AI_FOOD_RADIUS
        var best: Any? = null
        var bestScore = Float.MAX_VALUE
        // Prefer food ahead of us and food worth more.
        val hx = cos(s.heading)
        val hy = sin(s.heading)
        fun consider(obj: Any, x: Float, y: Float, value: Float) {
            val d2 = dist2(s.x, s.y, x, y)
            if (d2 > maxD2) return
            val ahead = ((x - s.x) * hx + (y - s.y) * hy) / sqrt(d2 + 1f)
            val score = d2 * (1.6f - ahead * 0.6f) / value
            if (score < bestScore) {
                bestScore = score
                best = obj
            }
        }
        for (p in sim.world.pellets) consider(p, p.x, p.y, p.value)
        for (t in sim.world.tailPieces) consider(t, t.x, t.y, t.value)
        return best
    }

    private fun foodHeading(s: Snake, brain: AiBrain): Float = when (val f = brain.food) {
        is Pellet -> if (f.gone) Float.NaN.also { brain.thinkTimer = 0f } else atan2(f.y - s.y, f.x - s.x)
        is TailPiece -> if (f.gone) Float.NaN.also { brain.thinkTimer = 0f } else atan2(f.y - s.y, f.x - s.x)
        else -> Float.NaN
    }

    private fun chaseHeading(sim: GameSim, s: Snake, brain: AiBrain, dt: Float): Float {
        val prey = sim.world.snakeById(brain.targetSnakeId)
        brain.chaseTime += dt
        if (prey == null || brain.chaseTime > GameConfig.AI_CHASE_MAX_SECONDS || s.relationTo(prey) != SizeRelation.BIGGER) {
            // Give up and rest for a bit: kids get a chance to escape.
            brain.chaseRest = GameConfig.AI_CHASE_REST_SECONDS
            brain.chaseTime = 0f
            brain.mode = AiMode.WANDER
            brain.thinkTimer = 0f
            return Float.NaN
        }
        // Aim a little ahead of where the prey is going.
        val d = dist(s.x, s.y, prey.x, prey.y)
        val lead = min(d * 0.5f, 120f)
        val tx = prey.x + cos(prey.heading) * lead
        val ty = prey.y + sin(prey.heading) * lead
        return atan2(ty - s.y, tx - s.x)
    }

    private fun wanderHeading(sim: GameSim, s: Snake, brain: AiBrain): Float {
        if (brain.wanderHeading.isNaN() || brain.wanderTimer <= 0f) {
            brain.wanderHeading = wrapAngle(s.heading + (sim.rng.nextFloat() - 0.5f) * 2.4f)
            brain.wanderTimer = 1f + sim.rng.nextFloat() * 2f
        }
        return brain.wanderHeading
    }

    /** Swerves away from the bodies of snakes we must not run into. */
    private fun avoidBodies(sim: GameSim, s: Snake, brain: AiBrain, desired: Float): Float {
        if (s.invulnerable > 0f) return desired
        val r = s.radius
        val look = 40f + r * 3f
        val px = s.x + cos(s.heading) * look
        val py = s.y + sin(s.heading) * look
        var hitX = 0f
        var hitY = 0f
        var found = false
        sim.grid.query(px, py, r + GameConfig.MAX_RADIUS + 10f) { o, seg ->
            if (!found && o !== s && !(brain.mode == AiMode.CHASE && o.id == brain.targetSnakeId) &&
                s.relationTo(o) != SizeRelation.BIGGER
            ) {
                val reach = r + o.segR[seg] + 10f
                if (dist2(px, py, o.segX[seg], o.segY[seg]) < reach * reach) {
                    hitX = o.segX[seg]
                    hitY = o.segY[seg]
                    found = true
                }
            }
        }
        if (!found) return desired
        // Turn to the side away from the obstacle.
        val cross = cos(s.heading) * (hitY - s.y) - sin(s.heading) * (hitX - s.x)
        return wrapAngle(s.heading + if (cross > 0f) -1.6f else 1.6f)
    }

    /** Blends the desired heading towards the middle when close to a wall. */
    private fun avoidWalls(sim: GameSim, s: Snake, desired: Float): Float {
        val margin = GameConfig.AI_WALL_AVOID + s.radius
        val w = sim.world.width
        val h = sim.world.height
        var px = 0f
        var py = 0f
        if (s.x < margin) px += (margin - s.x) / margin
        if (s.x > w - margin) px -= (s.x - (w - margin)) / margin
        if (s.y < margin) py += (margin - s.y) / margin
        if (s.y > h - margin) py -= (s.y - (h - margin)) / margin
        if (px == 0f && py == 0f) return desired
        val vx = cos(desired) + px * 3f
        val vy = sin(desired) + py * 3f
        return atan2(vy, vx)
    }
}
