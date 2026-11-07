package dev.lacruzjs.sobreio.di

/**
 * Entry point for Swift (`IosKoinKt.setUpKoin()`), called once when the iOS app starts. Not named `initKoin`:
 * Kotlin/Native would export a function starting with `init` as `doInitKoin`.
 */
fun setUpKoin() = initKoin()
