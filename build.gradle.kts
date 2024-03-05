import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val exposedVersion: String by project

plugins {
    kotlin("jvm") version "1.9.22"
    id("com.diffplug.spotless") version "6.25.0"
    id("io.ktor.plugin") version "2.3.9"
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

tasks.test {
    useJUnitPlatform()
}

spotless {
    kotlin {
        ktlint("1.1.1")
    }

    kotlinGradle {
        ktlint("1.1.1")
    }
}

tasks.withType<KotlinCompile>().configureEach {
    dependsOn("spotlessApply")
}

repositories {
    mavenCentral()
    maven { setUrl("https://github-package-registry-mirror.gc.nav.no/cached/maven-release") }
}

dependencies {
    implementation(libs.rapids.and.rivers)
    implementation(libs.konfig)
    implementation(libs.bundles.postgres)
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-crypt:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation("io.github.microutils:kotlin-logging:3.0.5")
    implementation("de.slub-dresden:urnlib:2.0.1")

    testImplementation(kotlin("test"))
    testImplementation(libs.bundles.kotest.assertions)
    testImplementation(libs.mockk)
    testImplementation(libs.bundles.postgres.test)
}
