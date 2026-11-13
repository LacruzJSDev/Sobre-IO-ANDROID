plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
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
        versionCode = 1
        versionName = "0.1.0"
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
