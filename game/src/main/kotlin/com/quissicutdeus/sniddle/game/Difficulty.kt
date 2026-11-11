package com.quissicutdeus.sniddle.game

import com.quissicutdeus.sniddle.game.model.Snail

/**
 * How computer snails go after the player: from how far away they notice
 * them, how likely they are to give chase, how long they keep at it, how long
 * they rest after giving up, and how far ahead of the player they aim.
 */
data class ChaseRules(
    val radius: Float,
    val chance: Float,
    val seconds: Float,
    val restSeconds: Float,
    val lead: Float,
)

/**
 * How hard a game is, chosen per kid (My Snail's "Little kid mode"). Every
 * number that differs between the modes lives here; everything else comes
 * from [GameConfig] whatever the mode.
 */
data class Difficulty(
    /** How it is saved (in a [com.quissicutdeus.sniddle.game.sim.SavedRun]); never change one. */
    val key: String,
    /** Hearts the player starts with. */
    val hearts: Int,
    /** Share of computer snails the spawner keeps smaller than the player; the rest are bigger. */
    val smallerFraction: Float,
    /** Food dots in the arena, as a multiple of the usual count. */
    val foodFactor: Float,
    /** How computer snails chase the player (chasing each other is the same in every mode). */
    val chaseYou: ChaseRules,
) {
    /** The rules for chasing [prey]: gentler for the player, as usual for everyone else. */
    fun chaseRulesFor(prey: Snail): ChaseRules = if (prey.isPlayer) chaseYou else NORMAL.chaseYou

    companion object {
        /** The game as it always was. */
        val NORMAL =
            Difficulty(
                key = "normal",
                hearts = GameConfig.PLAYER_HEARTS,
                smallerFraction = GameConfig.SMALLER_FRACTION,
                foodFactor = 1f,
                chaseYou =
                    ChaseRules(
                        radius = GameConfig.AI_CHASE_RADIUS,
                        chance = GameConfig.AI_AGGRESSION,
                        seconds = GameConfig.AI_CHASE_MAX_SECONDS,
                        restSeconds = GameConfig.AI_CHASE_REST_SECONDS,
                        lead = GameConfig.AI_CHASE_LEAD,
                    ),
            )

        /**
         * Little kid mode: the same game, just more forgiving. Measured in
         * LittleKidTest, a bot that never looks out for other snails loses
         * about a third as many hearts.
         *  - 4 hearts: one more bump before being gobbled.
         *  - 75% of computer snails smaller instead of 60%: about 3-4 of the
         *    14 are bigger instead of about 6, so there is more to eat and
         *    less to run from.
         *  - Half again as much food: growing (and so outgrowing the bigger
         *    snails) is quicker.
         *  - Bigger snails notice the player from 340 instead of 420 (about
         *    half a phone screen), start a chase half the time instead of 70%,
         *    give up after 3 s instead of 4 and rest 5 s instead of 3 before
         *    chasing again. They aim at where the player is, not ahead, so a
         *    turn gets away. They still chase, bite and gobble.
         *  - Not their speed: bigger snails are already slower, and steering
         *    clear of them is the game.
         */
        val LITTLE =
            Difficulty(
                key = "little",
                hearts = 4,
                smallerFraction = 0.75f,
                foodFactor = 1.5f,
                chaseYou = ChaseRules(radius = 340f, chance = 0.5f, seconds = 3f, restSeconds = 5f, lead = 0f),
            )

        /** The difficulty saved as [key]; null for one this version doesn't know. */
        fun byKey(key: String): Difficulty? = listOf(NORMAL, LITTLE).firstOrNull { it.key == key }
    }
}
