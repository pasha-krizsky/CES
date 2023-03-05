val cesVersion: String by project

val kotlinVersion: String by project
val ktorVersion: String by project
val koinVersion: String by project
val kotlinxDatetimeVersion: String by project
val minioVersion: String by project
val kotlinLoggingVersion: String by project
val logbackVersion: String by project
val kotestVersion: String by project
val kotestExtensionsVersion: String by project
val kotestAssertionsKtorVersion: String by project

plugins {
    kotlin("jvm") version "1.8.10"
    id("io.ktor.plugin") version "2.2.3"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.8.10"
}

group = "com.ces"
version = cesVersion

application {
    mainClass.set("io.ktor.server.cio.EngineMain")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":infrastructure:database"))
    implementation(project(":infrastructure:rabbitmq"))
    implementation(project(":infrastructure:minio"))

    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-cio-jvm:$ktorVersion")

    implementation("io.insert-koin:koin-ktor:$koinVersion")
    implementation("io.insert-koin:koin-logger-slf4j:$koinVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("io.github.microutils:kotlin-logging-jvm:$kotlinLoggingVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-datetime:$kotlinxDatetimeVersion")

    implementation("io.minio:minio:$minioVersion")

    testImplementation(testFixtures(project(":domain")))
    testImplementation(testFixtures(project(":infrastructure:database")))
    testImplementation(testFixtures(project(":infrastructure:rabbitmq")))
    testImplementation(testFixtures(project(":infrastructure:minio")))

    testImplementation("io.ktor:ktor-server-tests-jvm:$ktorVersion")
    testImplementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    testImplementation("io.ktor:ktor-server-test-host:2.2.3")
    testImplementation("io.kotest.extensions:kotest-assertions-ktor:$kotestAssertionsKtorVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-json:$kotestVersion")
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest.extensions:kotest-extensions-testcontainers:$kotestExtensionsVersion")
}