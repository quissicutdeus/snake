package com.quissicutdeus.sniddle.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

/**
 * Runs [onBack] for the phone's back gesture or button while [enabled]. The
 * one place the screens reach Android's back handling (the activity's), so
 * another platform only has to supply this.
 */
@Composable
internal fun SystemBack(
    enabled: Boolean = true,
    onBack: () -> Unit,
) = BackHandler(enabled, onBack)
