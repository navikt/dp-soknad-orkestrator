plugins {
    kotlin("jvm") version "2.0.10"
    id("org.openapi.generator") version "7.7.0"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.1"
}

tasks.named("compileKotlin").configure {
    dependsOn("openApiGenerate")
}

tasks.named("runKtlintCheckOverMainSourceSet").configure {
    dependsOn("openApiGenerate")
}

tasks.named("runKtlintFormatOverMainSourceSet").configure {
    dependsOn("openApiGenerate")
}

sourceSets {
    main {
        java {
            setSrcDirs(listOf("src/main/kotlin", "${layout.buildDirectory.get()}/generated/src/main/kotlin"))
        }
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.jackson.annotation)
}

configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    filter {
        exclude { element -> element.file.path.contains("generated/") }
        exclude { element -> element.file.path.contains("generated\\") }
    }
}

openApiGenerate {
    generatorName.set("kotlin-server")
    inputSpec.set("$projectDir/src/main/resources/soknad-orkestrator-api.yaml")
    outputDir.set("${layout.buildDirectory.get()}/generated/")
    packageName.set("no.nav.dagpenger.soknad.orkestrator.api")
    globalProperties.set(
        mapOf(
            "apis" to "none",
            "models" to "",
        ),
    )
    modelNameSuffix.set("DTO")
    configOptions.set(
        mapOf(
            "serializationLibrary" to "jackson",
            "enumPropertyNaming" to "original",
        ),
    )
}
