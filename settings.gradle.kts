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

rootProject.name = "Wherego"

include(
    ":app",
    ":core:model",
    ":core:database",
    ":core:datastore",
    ":core:common",
    ":core:i18n",
    ":core:designsystem",
    ":core:sync",
    ":feature:capture",
    ":feature:home",
    ":feature:stories",
    ":feature:plan",
    ":feature:settings",
    ":feature:auth",
)
