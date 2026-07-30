// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
}

subprojects {
    group = "cc.meteormc"
    version = "1.0.0"

    plugins.withType<com.android.build.gradle.LibraryPlugin> {
        extensions.configure<com.android.build.api.dsl.LibraryExtension> {
            publishing {
                singleVariant("release") {
                    withJavadocJar()
                    withSourcesJar()
                }
            }
        }
    }

    plugins.withId("maven-publish") {
        extensions.configure<PublishingExtension> {
            publications.withType<MavenPublication> {
                pom {
                    developers {
                        developer {
                            name.set("Meteor23333")
                            url.set("https://meteormc.cc")
                        }
                    }
                }
            }
        }
    }
}