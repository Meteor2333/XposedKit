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
    ":xposedkit-gradle-plugin",
    ":xposedkit-processor"
)
project(":xposedkit-gradle-plugin").projectDir = file("gradle-plugin")
project(":xposedkit-processor").projectDir = file("processor")