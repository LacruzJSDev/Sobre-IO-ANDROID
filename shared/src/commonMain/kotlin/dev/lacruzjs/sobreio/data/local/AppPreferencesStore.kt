package dev.lacruzjs.sobreio.data.local

import kotlinx.coroutines.flow.StateFlow

/** Preferences of the device (not of a user), kept locally. */
interface AppPreferencesStore {

    /** Money is shown in blue and orange instead of green and red. Read synchronously, so the first frame is right. */
    val colorBlindSafe: StateFlow<Boolean>

    fun setColorBlindSafe(enabled: Boolean)
}
