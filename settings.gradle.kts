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
        // *** ВИПРАВЛЕННЯ СИНТАКСИСУ НА KOTLIN DSL ***
        maven { url = uri("https://jitpack.io") }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // *** ВИПРАВЛЕННЯ СИНТАКСИСУ НА KOTLIN DSL ***
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "SilentCity"
include(":app")