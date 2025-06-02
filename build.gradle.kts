import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val exposedVersion: String by project

plugins {
    kotlin("jvm") version "2.1.21"
    id("io.ktor.plugin") version "3.1.3"
    id("org.jlleitschuh.gradle.ktlint") version "12.2.0"
    alias(libs.plugins.shadow.jar)
}

group = "no.nav"
version = "0.0.1"

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("no.nav.dagpenger.soknad.orkestrator.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile>().configureEach {
    dependsOn("ktlintFormat")
}

repositories {
    mavenCentral()
    maven { setUrl("https://github-package-registry-mirror.gc.nav.no/cached/maven-release") }
}

dependencies {
    implementation(project(path = ":openapi"))

    implementation(libs.rapids.and.rivers)
    implementation(libs.konfig)
    implementation(libs.bundles.postgres)
    implementation(libs.bundles.ktor.server)
    implementation(libs.bundles.ktor.client)
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-crypt:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-json:$exposedVersion")
    implementation("io.github.microutils:kotlin-logging:3.0.5")
    implementation("de.slub-dresden:urnlib:2.0.1")
    implementation("io.prometheus:prometheus-metrics-core:1.3.8")
    implementation("io.github.openhtmltopdf:openhtmltopdf-pdfbox:1.1.28")
    implementation("io.github.openhtmltopdf:openhtmltopdf-svg-support:1.1.28")
    implementation("no.nav.dagpenger:oauth2-klient:2025.04.26-14.51.bbf9ece5f5ec")

    implementation("io.ktor:ktor-server-netty:${libs.versions.ktor.get()}")
    implementation("io.ktor:ktor-server-config-yaml:${libs.versions.ktor.get()}")

    testImplementation(kotlin("test"))
    testImplementation(libs.bundles.kotest.assertions)
    testImplementation(libs.mockk)
    testImplementation(libs.mock.oauth2.server)
    testImplementation(libs.bundles.postgres.test)
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.12.2")
    testImplementation("io.ktor:ktor-server-test-host-jvm:${libs.versions.ktor.get()}")
    testImplementation(libs.rapids.and.rivers.test)
    testImplementation("com.github.navikt.tbd-libs:naisful-test-app:2025.04.04-09.18-7cc3badf")
}

tasks.withType<ShadowJar> {
    mergeServiceFiles()
}
