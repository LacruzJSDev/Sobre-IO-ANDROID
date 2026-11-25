plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// The app version lives in one place, `sobreio.version` in gradle.properties (SemVer, tag `v<version>`).
// versionCode grows with it: major * 10000 + minor * 100 + patch (1.0.0 -> 10000, 1.2.3 -> 10203).
val appVersion: String = providers.gradleProperty("sobreio.version").get()
val appVersionCode: Int = run {
    val parts = Regex("""(\d+)\.(\d+)\.(\d+)""").matchEntire(appVersion)?.destructured
        ?: error("sobreio.version must be <major>.<minor>.<patch>, found '$appVersion'")
    val (major, minor, patch) = parts.toList().map(String::toInt)
    require(minor < 100 && patch < 100) { "sobreio.version minor and patch must be below 100" }
    major * 10000 + minor * 100 + patch
}

android {
    namespace = "dev.lacruzjs.sobreio"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "dev.lacruzjs.sobreio"
        minSdk = 24
        targetSdk = 37
        versionCode = appVersionCode
        versionName = appVersion
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        buildConfig = true
    }
    androidResources {
        // The strings live in :shared (Compose resources), so Android cannot generate the locale list from res/values-*.
        // res/xml/locales_config.xml is written by hand and must list every language of the app.
        generateLocaleConfig = false
    }
}

dependencies {
    implementation(project(":shared"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
}

// The iOS app carries the same version: CFBundleShortVersionString is the version and CFBundleVersion the versionCode,
// both in iosApp/project.yml and in the Info.plist generated from it. CI runs this check on every pull request.
tasks.register("checkIosVersion") {
    group = "verification"
    description = "Fails when the iOS version differs from sobreio.version in gradle.properties."
    // Only plain values and providers are captured, so the task works with the configuration cache
    val root = rootProject.layout.projectDirectory
    val yml = providers.fileContents(root.file("iosApp/project.yml")).asText
    val plist = providers.fileContents(root.file("iosApp/iosApp/Info.plist")).asText
    val expectedName = appVersion
    val expectedCode = appVersionCode.toString()
    doLast {
        val ymlText = yml.get()
        val plistText = plist.get()
        val ymlValue = { key: String ->
            Regex("""$key:\s*"?([^"\s]+)"?""").find(ymlText)?.groupValues?.get(1)
        }
        val plistValue = { key: String ->
            Regex("""<key>$key</key>\s*<string>([^<]*)</string>""").find(plistText)?.groupValues?.get(1)
        }
        val found = mapOf(
            "project.yml CFBundleShortVersionString" to (ymlValue("CFBundleShortVersionString") to expectedName),
            "project.yml CFBundleVersion" to (ymlValue("CFBundleVersion") to expectedCode),
            "Info.plist CFBundleShortVersionString" to (plistValue("CFBundleShortVersionString") to expectedName),
            "Info.plist CFBundleVersion" to (plistValue("CFBundleVersion") to expectedCode),
        )
        val wrong = found.filter { (_, pair) -> pair.first != pair.second }
        check(wrong.isEmpty()) {
            wrong.entries.joinToString(prefix = "iOS version out of step with sobreio.version:\n", separator = "\n") {
                "  ${it.key} is ${it.value.first}, expected ${it.value.second}"
            }
        }
    }
}
