package com.quissicutdeus.sniddle

import androidx.compose.material3.Text
import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

/** The whole app for iOS, hosted by SniddleApp.swift. */
@Suppress("FunctionName") // Swift calls it by this name (see iosApp/project.yml).
fun MainViewController(): UIViewController = ComposeUIViewController { Text("Sniddle") }
