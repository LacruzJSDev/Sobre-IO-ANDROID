package dev.lacruzjs.sobreio

import android.app.Application
import dev.lacruzjs.sobreio.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.logger.Level

class SobreioApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.INFO else Level.NONE)
            androidContext(this@SobreioApplication)
        }
    }
}
