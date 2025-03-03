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
        maven("https://plugins.gradle.org/m2/")
        maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    }
}

//include(":godot.4.4.beta1")
// TODO: Update project's name.
rootProject.name = "LiveWallpaper"
include(":plugin")

//include(":myapplication")
//include(":godot4_4")
//include(":godot_4_4")
