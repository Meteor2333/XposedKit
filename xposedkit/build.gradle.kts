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
    compileOnly(libs.annotation)
    compileOnly(libs.lsposed.api)
    compileOnly(libs.xposed.api)
    api(libs.hidden.api.bypass)
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