package dev.lacruzjs.sobreio

import androidx.compose.ui.window.ComposeUIViewController

/** The Compose UI as a UIViewController, hosted by the SwiftUI app in iosApp. */
fun MainViewController() = ComposeUIViewController { App() }
