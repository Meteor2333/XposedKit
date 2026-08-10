plugins {
    `maven-publish`
    alias(libs.plugins.android.library)
}

android {
    namespace = "cc.meteormc.xposedkit"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }
}

dependencies {
    compileOnly(project(":xposedkit-hidden-api"))
    implementation(project(":xposedkit-native"))

    compileOnly(libs.annotation)
    api(libs.hidden.api.bypass)

    compileOnly(libs.xposed.api)
    compileOnly(libs.lsposed.api)
}

publishing {
    publications {
        register<MavenPublication>("release") {
            afterEvaluate {
                from(components["release"])
            }
        }
    }
}