// ── Redirect build outputs outside OneDrive to prevent sync file-locks ──────
// OneDrive locks files in build/ causing AccessDeniedException during Gradle tasks
val buildRoot = "C:/BuildOut/wavehouse"
gradle.allprojects {
    layout.buildDirectory.set(File("$buildRoot/${project.name}/build"))
}

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
    }
}

rootProject.name = "WaveHouse"
include(":app")
