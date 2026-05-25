pluginManagement {
    repositories {
        //clear()
        //maven { url 'https://maven.myket.ir' }
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
        //clear()
        //maven { url 'https://maven.myket.ir' }

        google()
        maven { url = uri("https://www.jitpack.io") } // این خط را به این شکل بنویس
        mavenCentral()
    }
}

rootProject.name = "MechanicApp"
include(":app")

