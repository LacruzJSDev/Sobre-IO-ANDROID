package dev.lacruzjs.sobreio

import kotlin.test.Test
import kotlin.test.assertTrue

class AppConfigTest {

    @Test
    fun appVersionIsTheSemVerOfGradleProperties() {
        assertTrue(
            Regex("""\d+\.\d+\.\d+""").matches(AppConfig.APP_VERSION),
            "APP_VERSION should be <major>.<minor>.<patch>, was '${AppConfig.APP_VERSION}'",
        )
    }
}
