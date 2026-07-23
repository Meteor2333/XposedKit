plugins {
    alias(libs.plugins.android.library)
}

group = "cc.meteormc"
version = "1.0.0"

android {
    namespace = "cc.meteormc.xposedkit"

    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

kotlin {
    jvmToolchain(11)
}

dependencies {
    compileOnly(libs.annotation)
    compileOnly(libs.lsposed.api)
    compileOnly(libs.xposed.api)
    api(libs.hidden.api.bypass)
}