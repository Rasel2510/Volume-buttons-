plugins {
    id("com.android.application")
}

android {
    namespace = "com.rasel.volumebuttons"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.rasel.volumebuttons"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            // Debug keys: the app is installed by hand, not from a store.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// No libraries: plain Android only, so the app stays tiny on low-memory phones.
