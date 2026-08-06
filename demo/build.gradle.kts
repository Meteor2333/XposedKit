plugins {
    alias(libs.plugins.android.application)
}

val commitCount
    get() = providers.exec {
        commandLine("git", "rev-list", "--count", "HEAD")
    }.standardOutput.asText.let {
        val text = it.orNull?.trim()
        if (text.isNullOrBlank()) return@let 0
        text.toIntOrNull() ?: 1
    }

android {
    namespace = "cc.meteormc.xposedkit.demo"

    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "cc.meteormc.xposedkit.demo"
        minSdk = 24
        targetSdk = 37
        versionCode = commitCount
        versionName = project.version.toString()
    }

    buildTypes {
        release {
            optimization {
                enable = true
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project)
    implementation(libs.core.ktx)
    implementation(libs.material)
}