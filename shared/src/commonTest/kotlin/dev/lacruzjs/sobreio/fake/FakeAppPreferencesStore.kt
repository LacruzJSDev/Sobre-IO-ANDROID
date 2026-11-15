package dev.lacruzjs.sobreio.fake

import dev.lacruzjs.sobreio.data.local.AppPreferencesStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeAppPreferencesStore(colorBlindSafe: Boolean = false) : AppPreferencesStore {

    private val _colorBlindSafe = MutableStateFlow(colorBlindSafe)
    override val colorBlindSafe: StateFlow<Boolean> = _colorBlindSafe

    override fun setColorBlindSafe(enabled: Boolean) {
        _colorBlindSafe.value = enabled
    }
}
