plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

group = "cc.meteormc"
version = "1.0.0"

kotlin {
    jvmToolchain(11)
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