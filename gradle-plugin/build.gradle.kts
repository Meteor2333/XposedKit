plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

group = "cc.meteormc"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    jvmToolchain(11)

    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
    }
}

dependencies {
    compileOnly(libs.android.build.tools)
    implementation(libs.devtools.ksp)
}

gradlePlugin {
    plugins {
        create("xposedkit") {
            id = "cc.meteormc.xposedkit.plugin"
            implementationClass = "cc.meteormc.xposedkit.plugin.XposedKitPlugin"
        }
    }
}