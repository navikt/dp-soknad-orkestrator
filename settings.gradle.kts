plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}
rootProject.name = "dp-soknad-orkestrator"
dependencyResolutionManagement {
    repositories {
        maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }
    versionCatalogs {
        create("libs") {
            from("no.nav.dagpenger:dp-version-catalog:20250224.132.e46261")
        }
    }
}
include("asyncapi")
include("openapi")
