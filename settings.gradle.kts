@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal()
        google()
        mavenCentral()
        maven{url=uri(File(rootDir,"local_repo"))}
    }
}

pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        google()
        mavenCentral()
        maven{url=uri(File(rootDir,"local_repo"))}
        flatDir{
            dirs = setOf( File("$rootDir/local_repo"))
        }
    }
}

include(":atox")
include(":core")
include(":domain")
