pluginManagement {
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

rootProject.name = "kaede-gallery-kotlin"

include(":core:model")
include(":core:repository")
include(":core:settings")
include(":core:rust")
include(":ui:app")
include(":desktopApp")
include(":androidApp")
