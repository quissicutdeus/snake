package com.quissicutdeus.snake.game.sim

import com.quissicutdeus.snake.game.GameConfig
import com.quissicutdeus.snake.game.model.PowerType
import com.quissicutdeus.snake.game.model.Snake

enum class SizeRelation { SMALLER, EQUAL, BIGGER }

/** How a snake of length [a] compares to one of length [b]. Within ~10% counts as equal. */
fun compareSize(
    a: Float,
    b: Float,
): SizeRelation =
    when {
        a > b * GameConfig.EQUAL_SIZE_RATIO -> SizeRelation.BIGGER
        b > a * GameConfig.EQUAL_SIZE_RATIO -> SizeRelation.SMALLER
        else -> SizeRelation.EQUAL
    }

fun Snake.relationTo(other: Snake): SizeRelation = compareSize(length, other.length)

/**
 * Who wins a fight: Super Strength beats any size (two strong snakes just
 * compare sizes). Use [relationTo] for the true size.
 */
fun Snake.fightRelationTo(other: Snake): SizeRelation {
    val mine = hasPower(PowerType.STRENGTH)
    val theirs = other.hasPower(PowerType.STRENGTH)
    return when {
        mine && !theirs -> SizeRelation.BIGGER
        theirs && !mine -> SizeRelation.SMALLER
        else -> relationTo(other)
    }
}
