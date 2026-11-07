package dev.lacruzjs.sobreio.di

import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

/** Starts Koin with every module of the app; each platform adds its own pieces (Android context, logger) in [config]. */
fun initKoin(config: KoinAppDeclaration = {}) {
    startKoin {
        config()
        modules(appModules)
    }
}
