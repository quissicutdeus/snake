package com.quissicutdeus.sniddle.game.sim

import com.quissicutdeus.sniddle.game.Difficulty
import com.quissicutdeus.sniddle.game.GameConfig
import com.quissicutdeus.sniddle.game.model.Food
import com.quissicutdeus.sniddle.game.model.PowerType
import com.quissicutdeus.sniddle.game.model.Snail
import com.quissicutdeus.sniddle.game.model.dist
import com.quissicutdeus.sniddle.game.model.dist2
import com.quissicutdeus.sniddle.game.model.wrapAngle
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

enum class AiMode { WANDER, FOOD, CHASE, FLEE }

/** Per-snail AI memory. */
class AiBrain(
    rng: Random,
) {
    var mode = AiMode.WANDER
        internal set
    internal var thinkTimer = rng.nextFloat() * GameConfig.AI_THINK_INTERVAL
    internal var wanderHeading = Float.NaN
    internal var wanderTimer = 0f
    internal var targetSnailId = -1
    internal var food: Food? = null
    internal var fleeHeading = 0f
    internal var fleeFromId = -1
        private set
    internal var fleeTimer = 0f
    internal var chaseTime = 0f
    internal var chaseRest = 0f

    /** Run away from [threat] for a while (after being bitten or bumped). */
    fun startFleeing(threat: Snail) {
        fleeFromId = threat.id
        fleeTimer = GameConfig.AI_FLEE_SECONDS
        mode = AiMode.FLEE
    }
}

/**
 * What an AI sees around it when it thinks. Each [GameSim] owns one and
 * refills it for every snail that thinks, so thinking allocates nothing and
 * two games never share it.
 */
internal class AiScan {
    var fleeX = 0f
    var fleeY = 0f
    var threatened = false
    var prey: Snail? = null
    var preyDistance = Float.MAX_VALUE
}

/** Wander -> seek food -> chase smaller snails -> flee bigger ones. Same rules as the player. */
object Ai {
    /** Returns the heading this AI snail wants this step. */
    fun desiredHeading(
        sim: GameSim,
        s: Snail,
        dt: Float,
    ): Float {
        val brain = s.brain ?: return Float.NaN
        brain.chaseRest = (brain.chaseRest - dt).coerceAtLeast(0f)
        brain.fleeTimer = (brain.fleeTimer - dt).coerceAtLeast(0f)
        brain.wanderTimer -= dt
        brain.thinkTimer -= dt
        if (brain.thinkTimer <= 0f) {
            brain.thinkTimer += GameConfig.AI_THINK_INTERVAL
            think(sim, s, brain)
        }

        var desired =
            when (brain.mode) {
                AiMode.FLEE -> brain.fleeHeading
                AiMode.CHASE -> chaseHeading(sim, s, brain, dt)
                AiMode.FOOD -> foodHeading(s, brain)
                AiMode.WANDER -> wanderHeading(sim, s, brain)
            }
        if (desired.isNaN()) desired = wanderHeading(sim, s, brain)
        desired = avoidBodies(sim, s, brain, desired)
        return avoidWalls(sim, s, desired)
    }

    /** Picks a mode: flee threats, else chase prey, else go for food, else wander. */
    private fun think(
        sim: GameSim,
        s: Snail,
        brain: AiBrain,
    ) {
        val scan = scan(sim, s)
        when {
            scan.threatened -> {
                brain.mode = AiMode.FLEE
                brain.fleeHeading = atan2(scan.fleeY, scan.fleeX)
                brain.chaseTime = 0f
            }

            brain.fleeTimer > 0f -> {
                keepFleeing(sim, s, brain)
            }

            !tryChase(sim, brain, scan.prey) -> {
                seekFoodOrWander(sim, s, brain)
            }
        }
    }

    /** Fills the sim's [AiScan] with the combined direction away from threats and the nearest prey. */
    private fun scan(
        sim: GameSim,
        s: Snail,
    ): AiScan {
        val scan = sim.aiScan
        scan.fleeX = 0f
        scan.fleeY = 0f
        scan.threatened = false
        scan.prey = null
        scan.preyDistance = Float.MAX_VALUE
        for (o in sim.world.snails) {
            if (o === s || !o.alive) continue
            val d = dist(s.x, s.y, o.x, o.y).coerceAtLeast(1f)
            when (s.fightRelationTo(o)) {
                SizeRelation.SMALLER -> {
                    noteThreat(scan, s, o, d)
                }

                SizeRelation.BIGGER -> {
                    notePrey(scan, o, d, sim.difficulty.chaseRulesFor(o).radius)
                }

                SizeRelation.EQUAL -> {}
            }
        }
        return scan
    }

    private fun noteThreat(
        scan: AiScan,
        s: Snail,
        threat: Snail,
        d: Float,
    ) {
        val noticed = d < GameConfig.AI_FLEE_RADIUS + threat.radius * GameConfig.AI_FLEE_SIZE_FACTOR
        if (!noticed || s.invulnerable > 0f) return
        // Closer threats count more.
        val w = 1f / d
        scan.fleeX += (s.x - threat.x) / d * w
        scan.fleeY += (s.y - threat.y) / d * w
        scan.threatened = true
    }

    private fun notePrey(
        scan: AiScan,
        prey: Snail,
        d: Float,
        chaseRadius: Float,
    ) {
        // Blinking or armored snails can't be hurt, so they aren't worth chasing.
        val catchable = prey.invulnerable <= 0f && !prey.hasPower(PowerType.ARMOR)
        if (d < chaseRadius && d < scan.preyDistance && catchable) {
            scan.prey = prey
            scan.preyDistance = d
        }
    }

    private fun keepFleeing(
        sim: GameSim,
        s: Snail,
        brain: AiBrain,
    ) {
        val t = sim.world.snailById(brain.fleeFromId)
        if (t != null) brain.fleeHeading = atan2(s.y - t.y, s.x - t.x)
        brain.mode = AiMode.FLEE
    }

    /** Starts or continues a chase; returns false if this AI isn't chasing anyone. */
    private fun tryChase(
        sim: GameSim,
        brain: AiBrain,
        prey: Snail?,
    ): Boolean {
        if (prey == null || brain.chaseRest > 0f) return false
        val alreadyChasing = brain.mode == AiMode.CHASE && brain.targetSnailId == prey.id
        if (alreadyChasing || sim.rng.nextFloat() < sim.difficulty.chaseRulesFor(prey).chance) {
            if (!alreadyChasing) brain.chaseTime = 0f
            brain.mode = AiMode.CHASE
            brain.targetSnailId = prey.id
            return true
        }
        // Decided not to chase this time; don't reconsider for a moment.
        brain.chaseRest = GameConfig.AI_HESITATE_SECONDS
        return false
    }

    private fun seekFoodOrWander(
        sim: GameSim,
        s: Snail,
        brain: AiBrain,
    ) {
        if (brain.mode == AiMode.CHASE) brain.chaseTime = 0f
        val food = nearestFood(sim, s)
        if (food != null) {
            brain.mode = AiMode.FOOD
            brain.food = food
        } else {
            brain.mode = AiMode.WANDER
        }
    }

    private fun nearestFood(
        sim: GameSim,
        s: Snail,
    ): Food? {
        val maxD2 = GameConfig.AI_FOOD_RADIUS * GameConfig.AI_FOOD_RADIUS
        var best: Food? = null
        var bestScore = Float.MAX_VALUE
        // Prefer food ahead of us and food worth more.
        val hx = cos(s.heading)
        val hy = sin(s.heading)

        fun consider(f: Food) {
            val d2 = dist2(s.x, s.y, f.x, f.y)
            if (d2 > maxD2) return
            val ahead = ((f.x - s.x) * hx + (f.y - s.y) * hy) / sqrt(d2 + 1f)
            val score = d2 * (1.6f - ahead * 0.6f) / f.value
            if (score < bestScore) {
                bestScore = score
                best = f
            }
        }
        sim.world.pellets.forEach(::consider)
        sim.world.tailPieces.forEach(::consider)
        return best
    }

    private fun foodHeading(
        s: Snail,
        brain: AiBrain,
    ): Float {
        val f = brain.food
        if (f == null || f.gone) {
            brain.thinkTimer = 0f // look for something else
            return Float.NaN
        }
        return atan2(f.y - s.y, f.x - s.x)
    }

    private fun chaseHeading(
        sim: GameSim,
        s: Snail,
        brain: AiBrain,
        dt: Float,
    ): Float {
        val prey = sim.world.snailById(brain.targetSnailId)
        val rules = prey?.let { sim.difficulty.chaseRulesFor(it) } ?: Difficulty.NORMAL.chaseYou
        brain.chaseTime += dt
        if (prey == null || brain.chaseTime > rules.seconds || s.fightRelationTo(prey) != SizeRelation.BIGGER) {
            // Give up and rest for a bit: kids get a chance to escape.
            brain.chaseRest = rules.restSeconds
            brain.chaseTime = 0f
            brain.mode = AiMode.WANDER
            brain.thinkTimer = 0f
            return Float.NaN
        }
        // Aim a little ahead of where the prey is going.
        val d = dist(s.x, s.y, prey.x, prey.y)
        val lead = min(d * 0.5f, rules.lead)
        val tx = prey.x + cos(prey.heading) * lead
        val ty = prey.y + sin(prey.heading) * lead
        return atan2(ty - s.y, tx - s.x)
    }

    private fun wanderHeading(
        sim: GameSim,
        s: Snail,
        brain: AiBrain,
    ): Float {
        if (brain.wanderHeading.isNaN() || brain.wanderTimer <= 0f) {
            brain.wanderHeading = wrapAngle(s.heading + (sim.rng.nextFloat() * 2f - 1f) * GameConfig.AI_WANDER_TURN / 2f)
            brain.wanderTimer = GameConfig.AI_WANDER_MIN_SECONDS + sim.rng.nextFloat() * GameConfig.AI_WANDER_EXTRA_SECONDS
        }
        return brain.wanderHeading
    }

    /** Bodies an AI must not run into: anything not clearly smaller, except prey it is chasing. */
    private fun mustAvoid(
        s: Snail,
        o: Snail,
        brain: AiBrain,
    ): Boolean {
        val chasingIt = brain.mode == AiMode.CHASE && o.id == brain.targetSnailId
        return o !== s && !chasingIt && s.fightRelationTo(o) != SizeRelation.BIGGER
    }

    /** Swerves away from the bodies of snails we must not run into. */
    private fun avoidBodies(
        sim: GameSim,
        s: Snail,
        brain: AiBrain,
        desired: Float,
    ): Float {
        if (s.invulnerable > 0f) return desired
        val r = s.radius
        val look = 40f + r * 3f
        val px = s.x + cos(s.heading) * look
        val py = s.y + sin(s.heading) * look
        var hitX = 0f
        var hitY = 0f
        var found = false
        sim.grid.query(px, py, r + GameConfig.MAX_RADIUS + GameConfig.AI_AVOID_MARGIN) { o, seg ->
            if (!found && mustAvoid(s, o, brain)) {
                val reach = r + o.segR[seg] + GameConfig.AI_AVOID_MARGIN
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
        val swerve = if (cross > 0f) -GameConfig.AI_SWERVE_ANGLE else GameConfig.AI_SWERVE_ANGLE
        return wrapAngle(s.heading + swerve)
    }

    /** Blends the desired heading towards the middle when close to a wall. */
    private fun avoidWalls(
        sim: GameSim,
        s: Snail,
        desired: Float,
    ): Float {
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
        val vx = cos(desired) + px * GameConfig.AI_WALL_PUSH
        val vy = sin(desired) + py * GameConfig.AI_WALL_PUSH
        return atan2(vy, vx)
    }
}
