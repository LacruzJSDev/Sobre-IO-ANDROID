package dev.lacruzjs.sobreio.data.local

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** [AppPreferencesStore] on the platform key-value store (SharedPreferences, NSUserDefaults). */
class SettingsAppPreferencesStore(private val settings: Settings) : AppPreferencesStore {

    private val _colorBlindSafe = MutableStateFlow(settings.getBoolean(KEY_COLOR_BLIND_SAFE, false))
    override val colorBlindSafe: StateFlow<Boolean> = _colorBlindSafe.asStateFlow()

    override fun setColorBlindSafe(enabled: Boolean) {
        settings.putBoolean(KEY_COLOR_BLIND_SAFE, enabled)
        _colorBlindSafe.value = enabled
    }

    private companion object {
        const val KEY_COLOR_BLIND_SAFE = "color_blind_safe"
    }
}
