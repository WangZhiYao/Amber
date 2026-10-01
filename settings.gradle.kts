pluginManagement {
    includeBuild("build-logic")

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
    }
}

rootProject.name = "Amber"
include(":app")

include(":core:common")
include(":core:database")
include(":core:ble")

include(":data:device")

include(":domain:device")
include(":domain:light")
include(":domain:clock")

include(":feature:clock")
include(":feature:light")
include(":feature:settings")

include(":shared:designsystem")
include(":shared:ui")
