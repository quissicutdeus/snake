package com.quissicutdeus.sniddle.ui

import androidx.compose.runtime.Composable

/**
 * Runs [onBack] for the system's back gesture or button while [enabled]. The
 * one place the screens reach the platform's back handling. iPhones and iPads
 * have no back button, so every screen also has a visible way back.
 */
@Composable
internal expect fun SystemBack(
    enabled: Boolean = true,
    onBack: () -> Unit,
)
