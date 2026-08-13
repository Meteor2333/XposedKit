pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://api.xposed.info")
    }
}

rootProject.name = "XposedKit"
include(
    ":xposedkit",
    ":xposedkit-bridge",
    ":xposedkit-gradle-plugin",
    ":xposedkit-hidden-api",
    ":xposedkit-jadx-plugin",
    ":xposedkit-native",
    ":xposedkit-processor"
)
project(":xposedkit-bridge").projectDir = file("bridge")
project(":xposedkit-gradle-plugin").projectDir = file("gradle-plugin")
project(":xposedkit-hidden-api").projectDir = file("hidden-api")
project(":xposedkit-jadx-plugin").projectDir = file("jadx-plugin")
project(":xposedkit-native").projectDir = file("native")
project(":xposedkit-processor").projectDir = file("processor")