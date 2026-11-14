package com.quissicutdeus.sniddle.ui

import androidx.compose.runtime.Composable

/** iPhones and iPads have no back button: every screen has a visible way back instead. */
@Composable
internal actual fun SystemBack(
    enabled: Boolean,
    onBack: () -> Unit,
) = Unit
