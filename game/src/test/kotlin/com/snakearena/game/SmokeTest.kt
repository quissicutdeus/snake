package com.snakearena.game

import kotlin.test.Test
import kotlin.test.assertNotNull

class SmokeTest {
    @Test fun configExists() {
        assertNotNull(GameConfig)
    }
}
