import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val exposedVersion: String by project
val pamGeographyVersion: String by project
val pdlVersion: String by project
val kotlinLoggingVersion: String by project
val urnlibVersion: String by project
val prometheusMetricsCoreVersion: String by project
val openHtmlToPdfVersion: String by project
val dagpengerOauth2KlientVersion: String by project

val junitJupiterVersion: String by project
val naisfulTestAppVersion: String by project

plugins {
    kotlin("jvm") version "2.2.20"
    id("io.ktor.plugin") version "3.3.1"
    id("org.jlleitschuh.gradle.ktlint") version "13.1.0"
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
    implementation(libs.kotlin.logging)
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-crypt:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-json:$exposedVersion")
    implementation("de.slub-dresden:urnlib:$urnlibVersion")
    implementation("io.prometheus:prometheus-metrics-core:$prometheusMetricsCoreVersion")
    implementation("io.github.openhtmltopdf:openhtmltopdf-pdfbox:$openHtmlToPdfVersion")
    implementation("io.github.openhtmltopdf:openhtmltopdf-svg-support:$openHtmlToPdfVersion")
    implementation("no.nav.dagpenger:oauth2-klient:$dagpengerOauth2KlientVersion")
    implementation("no.nav.pam.geography:pam-geography:$pamGeographyVersion")
    implementation("no.nav.dagpenger:pdl-klient:$pdlVersion")

    implementation("io.ktor:ktor-server-netty:${libs.versions.ktor.get()}")
    implementation("io.ktor:ktor-server-config-yaml:${libs.versions.ktor.get()}")

    testImplementation(kotlin("test"))
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.bundles.kotest.assertions)
    testImplementation(libs.mockk)
    testImplementation(libs.mock.oauth2.server)
    testImplementation(libs.bundles.postgres.test)
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitJupiterVersion")
    testImplementation("io.ktor:ktor-server-test-host-jvm:${libs.versions.ktor.get()}")
    testImplementation(libs.rapids.and.rivers.test)
    testImplementation("com.github.navikt.tbd-libs:naisful-test-app:$naisfulTestAppVersion")
}
