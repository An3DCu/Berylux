pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("androidx.*")
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google {
            content {
                includeGroupByRegex("androidx.*")
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
            }
        }
        mavenCentral()
    }
}

plugins {
    // Lets Gradle download the JDK named in gradle/gradle-daemon-jvm.properties.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "Berylux"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":app")

include(":core:model")
include(":core:domain")
include(":core:database")
include(":core:datastore")
include(":core:data")
include(":core:navigation")
include(":core:designsystem")
include(":core:ui")
include(":core:testing")

include(":feature:today")
include(":feature:tracker")
include(":feature:catalog")
include(":feature:editor")
include(":feature:progress")
include(":feature:profile")
include(":feature:settings")
include(":feature:rivals")
