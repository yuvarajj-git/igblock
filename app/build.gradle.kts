plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.igcap"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.igcap"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        debug {
            // TEST BUILD: 2 minute limit. Debug builds are "testOnly", so the device owner
            // can be removed with: adb shell dpm remove-active-admin com.example.igcap/.AdminReceiver
            buildConfigField("long", "LIMIT_MINUTES", "2L")
        }
        release {
            // REAL BUILD: 60 minute limit. Cannot be removed with adb. Only a factory reset removes it.
            buildConfigField("long", "LIMIT_MINUTES", "60L")
            signingConfig = signingConfigs.getByName("debug") // debug key, only so the APK is installable
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}
