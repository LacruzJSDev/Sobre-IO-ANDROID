package dev.lacruzjs.sobreio.navigation

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/**
 * Tells Navigation 3 how to save the back stack on platforms without reflection (iOS). Every [NavKey] of
 * Routes.kt must be registered here, or the back stack cannot be restored.
 */
internal val NavSavedStateConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(SignIn::class)
            subclass(SignUp::class)
            subclass(ConfirmEmail::class)
            subclass(PasswordRecovery::class)
            subclass(Home::class)
        }
    }
}
