package com.quissicutdeus.sniddle.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

/** Android's back gesture and button, through the activity. */
@Composable
internal actual fun SystemBack(
    enabled: Boolean,
    onBack: () -> Unit,
) = BackHandler(enabled, onBack)
