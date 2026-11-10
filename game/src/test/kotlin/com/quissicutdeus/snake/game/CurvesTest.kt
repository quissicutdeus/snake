package com.quissicutdeus.snake.game

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CurvesTest {
    @Test fun speedStrictlyDecreasesWithLengthAndNeverDropsBelowFloor() {
        val floor = GameConfig.MAX_SPEED * GameConfig.SPEED_FLOOR_FRACTION
        var prev = Float.MAX_VALUE
        var length = GameConfig.MIN_LENGTH
        while (length < 100_000f) {
            val v = GameConfig.speedFor(length)
            assertTrue(v < prev, "speed must fall: len=$length v=$v prev=$prev")
            assertTrue(v >= floor, "speed $v below floor at len=$length")
            prev = v
            length *= 1.15f
        }
        assertEquals(GameConfig.MAX_SPEED, GameConfig.speedFor(GameConfig.MIN_LENGTH), 0.001f)
        assertTrue(GameConfig.speedFor(1e9f) >= floor)
    }

    @Test fun biggerSnakesLookThickerButStayWithinLimits() {
        var prev = 0f
        for (len in listOf(60f, 150f, 400f, 1000f, 3000f)) {
            val r = GameConfig.radiusFor(len)
            assertTrue(r > prev, "radius grows with length")
            prev = r
        }
        assertEquals(GameConfig.START_RADIUS, GameConfig.radiusFor(GameConfig.PLAYER_START_LENGTH), 0.01f)
        assertEquals(GameConfig.MAX_RADIUS, GameConfig.radiusFor(1e7f))
    }

    @Test fun scoreIsLengthInPoints() {
        assertEquals(30, GameConfig.scoreFor(GameConfig.PLAYER_START_LENGTH))
    }
}
