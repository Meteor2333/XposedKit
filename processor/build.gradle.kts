plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ksp)
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
    implementation(libs.symbol.processing.api)
}