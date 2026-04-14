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

rootProject.name = "GithubViewer"

include(":app")
include(":core:common")
include(":core:network")
include(":core:database")
include(":data")
include(":presentation")
include(":integration-test")
