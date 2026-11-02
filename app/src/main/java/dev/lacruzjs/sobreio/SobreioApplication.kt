package dev.lacruzjs.sobreio

import android.app.Application
import dev.lacruzjs.sobreio.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class SobreioApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.INFO else Level.NONE)
            androidContext(this@SobreioApplication)
            modules(appModules)
        }
    }
}
