package com.snakearena.game

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Every gameplay tunable in one place. Distances are world units, times are
 * seconds, speeds are world units per second, angles are radians.
 */
object GameConfig {
    /** Fixed simulation timestep (60 Hz). */
    const val STEP = 1f / 60f

    /** Never run more than this many steps per frame (avoids a spiral after a hitch). */
    const val MAX_STEPS_PER_FRAME = 5

    // ---- Arena -------------------------------------------------------------
    const val ARENA_WIDTH = 4000f
    const val ARENA_HEIGHT = 4000f

    /** After hitting a wall, steering is ignored this long so you slide off it gently. */
    const val WALL_BOUNCE_SECONDS = 0.2f

    // ---- Size --------------------------------------------------------------
    /** No snake is ever shorter than this. */
    const val MIN_LENGTH = 60f
    const val PLAYER_START_LENGTH = 150f

    /** Body radius of a snake at [PLAYER_START_LENGTH]; thickness grows with length. */
    const val START_RADIUS = 12f
    const val RADIUS_GROWTH_EXPONENT = 0.4f
    const val MIN_RADIUS = 8f
    const val MAX_RADIUS = 48f

    /** Score shown to players = length / this (so a starting snake is "15"). */
    const val LENGTH_PER_POINT = 10f

    /** Sizes within this ratio of each other count as "the same size": both just bounce. */
    const val EQUAL_SIZE_RATIO = 1.10f

    // ---- Movement ----------------------------------------------------------
    /** Speed at [MIN_LENGTH]. */
    const val MAX_SPEED = 300f

    /** Speed never drops below MAX_SPEED * this, however long a snake gets. */
    const val SPEED_FLOOR_FRACTION = 0.5f
    const val SPEED_FALLOFF_EXPONENT = 0.5f

    /** AI snakes move a little slower than the player at the same size (kid-friendly). */
    const val AI_SPEED_FACTOR = 0.92f

    /** Maximum turning speed. */
    const val TURN_RATE = 4.2f

    /** A new trail point is recorded whenever the head has moved this far. */
    const val TRAIL_SPACING = 3f

    /** Body circles are sampled every radius * this along the body. */
    const val SEGMENT_SPACING_FACTOR = 0.5f

    /** The last part of the body (as a fraction of length) tapers towards the tail tip. */
    const val TAIL_TAPER_START = 0.65f
    const val TAIL_TIP_SCALE = 0.45f

    // ---- Food --------------------------------------------------------------
    const val PELLET_COUNT = 520
    const val PELLET_VALUE = 10f
    const val PELLET_RADIUS = 6f
    const val BIG_PELLET_CHANCE = 0.12f
    const val BIG_PELLET_VALUE = 30f
    const val BIG_PELLET_RADIUS = 10f

    /** Food this close to a head (beyond touching) is eaten. */
    const val EAT_MARGIN = 6f

    /** Food within head radius * 2 + this drifts towards the head. */
    const val SUCTION_RANGE = 36f
    const val SUCTION_SPEED = 260f

    /** Dropped tail pieces lie on the ground this long before fading away. */
    const val TAIL_PIECE_LIFETIME = 60f
    const val TAIL_PIECE_FADE = 3f
    const val MAX_TAIL_PIECES = 700

    /** Dropped tail is split into pieces roughly this many victim radii apart. */
    const val TAIL_PIECE_SPACING_FACTOR = 1.6f
    const val TAIL_PIECE_RADIUS_FACTOR = 0.75f

    // ---- Snake vs snake ----------------------------------------------------
    /** Eating a snake whole grows you by this fraction of its length. */
    const val EAT_WHOLE_GAIN = 0.5f

    const val PLAYER_HEARTS = 3
    const val AI_HEARTS = 1

    /** Blinking invulnerability after losing a heart. */
    const val HURT_INVULNERABLE_SECONDS = 1.5f

    /** Short protection after losing your tail, so one bite can't chew through the whole snake. */
    const val CUT_PROTECT_SECONDS = 0.8f

    /** Steering is ignored this long after a bounce, so you fly back a bit. */
    const val BOUNCE_SECONDS = 0.3f
    const val BOUNCE_SPEED_FACTOR = 1.3f

    // ---- AI ----------------------------------------------------------------
    const val AI_THINK_INTERVAL = 0.15f
    const val AI_FLEE_RADIUS = 300f
    const val AI_CHASE_RADIUS = 420f
    const val AI_FOOD_RADIUS = 380f
    const val AI_WALL_AVOID = 180f

    /** Chance that an AI decides to chase prey it sees (0..1). */
    const val AI_AGGRESSION = 0.7f

    /** AI gives up a chase after this long, then rests before chasing again. */
    const val AI_CHASE_MAX_SECONDS = 4f
    const val AI_CHASE_REST_SECONDS = 3f

    // ---- Spawner -----------------------------------------------------------
    const val AI_TARGET_COUNT = 14
    const val SPAWN_INTERVAL = 0.7f

    /** Share of AI snakes that should be smaller than the player. */
    const val SMALLER_FRACTION = 0.6f
    const val SMALLER_MIN_RATIO = 0.4f
    const val SMALLER_MAX_RATIO = 0.82f
    const val BIGGER_MIN_RATIO = 1.25f
    const val BIGGER_MAX_RATIO = 2.3f

    /** Off-screen AI snakes whose size ratio to the player leaves this range are removed. */
    const val DESPAWN_MIN_RATIO = 0.15f
    const val DESPAWN_MAX_RATIO = 4f

    // ---- Camera ------------------------------------------------------------
    /** World units visible across the short side of the screen at the start. */
    const val BASE_VIEW_SPAN = 720f
    const val VIEW_GROWTH_EXPONENT = 0.55f
    const val MAX_VIEW_SPAN = 2000f

    /** Widest screen aspect ratio we plan for (used to keep spawns off-screen). */
    const val MAX_ASPECT = 2.4f

    // ---- Curves ------------------------------------------------------------

    fun radiusFor(length: Float): Float {
        val r = START_RADIUS * (max(length, MIN_LENGTH) / PLAYER_START_LENGTH).pow(RADIUS_GROWTH_EXPONENT)
        return r.coerceIn(MIN_RADIUS, MAX_RADIUS)
    }

    /** Bigger = slower: full speed at [MIN_LENGTH], falling smoothly towards the floor. */
    fun speedFor(length: Float): Float {
        val l = max(length, MIN_LENGTH)
        val falloff = (MIN_LENGTH / l).pow(SPEED_FALLOFF_EXPONENT)
        return MAX_SPEED * (SPEED_FLOOR_FRACTION + (1f - SPEED_FLOOR_FRACTION) * falloff)
    }

    fun scoreFor(length: Float): Int = (length / LENGTH_PER_POINT).roundToInt()

    /** World units visible across the short side of the screen for a snake of this radius. */
    fun viewSpanFor(radius: Float): Float =
        min(MAX_VIEW_SPAN, BASE_VIEW_SPAN * (radius / START_RADIUS).pow(VIEW_GROWTH_EXPONENT))

    /** Distance from the player beyond which a point is surely off-screen. */
    fun offscreenDistance(playerRadius: Float): Float {
        val span = viewSpanFor(playerRadius)
        val halfDiagonal = span / 2f * kotlin.math.sqrt(1f + MAX_ASPECT * MAX_ASPECT)
        return halfDiagonal + 2f * MAX_RADIUS
    }
}
