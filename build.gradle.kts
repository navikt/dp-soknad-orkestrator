val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project

plugins {
    kotlin("jvm") version "1.9.22"
    id("com.diffplug.spotless") version "6.20.0"
    id("io.ktor.plugin") version "2.3.8"
}

group = "no.nav"
version = "0.0.1"

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("no.nav.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
    maven { setUrl("https://github-package-registry-mirror.gc.nav.no/cached/maven-release") }
}

dependencies {
    implementation("com.github.navikt:rapids-and-rivers:2023120410321701682379.55cc1a24d803")
    implementation("com.natpryce:konfig:1.6.10.0")

    testImplementation(kotlin("test"))
    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
    testImplementation("io.kotest:kotest-assertions-core:5.8.0")
    testImplementation("io.kotest:kotest-property:5.8.0")
}

tasks.test {
    useJUnitPlatform()
}