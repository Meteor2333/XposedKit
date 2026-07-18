plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ksp)
}

group = "cc.meteormc"
version = "1.0.0"

kotlin {
    jvmToolchain(11)
}

dependencies {
    implementation(libs.symbol.processing.api)
}