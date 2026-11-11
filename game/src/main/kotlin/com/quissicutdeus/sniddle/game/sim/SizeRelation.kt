package com.quissicutdeus.sniddle.game.sim

import com.quissicutdeus.sniddle.game.GameConfig
import com.quissicutdeus.sniddle.game.model.PowerType
import com.quissicutdeus.sniddle.game.model.Snail

enum class SizeRelation { SMALLER, EQUAL, BIGGER }

/** How a snail of length [a] compares to one of length [b]. Within ~10% counts as equal. */
fun compareSize(
    a: Float,
    b: Float,
): SizeRelation =
    when {
        a > b * GameConfig.EQUAL_SIZE_RATIO -> SizeRelation.BIGGER
        b > a * GameConfig.EQUAL_SIZE_RATIO -> SizeRelation.SMALLER
        else -> SizeRelation.EQUAL
    }

fun Snail.relationTo(other: Snail): SizeRelation = compareSize(length, other.length)

/**
 * Who wins a fight: Super Strength beats any size (two strong snails just
 * compare sizes). Use [relationTo] for the true size.
 */
fun Snail.fightRelationTo(other: Snail): SizeRelation {
    val mine = hasPower(PowerType.STRENGTH)
    val theirs = other.hasPower(PowerType.STRENGTH)
    return when {
        mine && !theirs -> SizeRelation.BIGGER
        theirs && !mine -> SizeRelation.SMALLER
        else -> relationTo(other)
    }
}
