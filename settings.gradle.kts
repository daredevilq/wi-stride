pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
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

rootProject.name = "WiStride"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":app")
include(":core:common")
include(":core:datastore")
include(":core:designsystem")
include(":core:model")
include(":core:network")
include(":core:sensors")
include(":core:testing")
include(":core:ui")
include(":core:wifi")
include(":domain:network")
include(":domain:wifi")
include(":feature:map")
include(":feature:networks")
include(":feature:now")
include(":feature:ping")
include(":feature:speed")
