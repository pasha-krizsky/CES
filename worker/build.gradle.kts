val cesVersion: String by project
val kotlinVersion: String by project
val ktorVersion: String by project
val logbackVersion: String by project
val junitVersion: String by project

plugins {
    id("kotlin")
    id("io.ktor.plugin") version "2.2.3"
    kotlin("plugin.serialization") version "1.8.10"
}

group = "com.ces"
version = cesVersion

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
    implementation("org.apache.commons:commons-compress:1.22")
    implementation("io.netty:netty-all:4.1.87.Final")
    implementation("com.rabbitmq:amqp-client:5.16.0")

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
}

tasks.jar {
    manifest.attributes["Main-Class"] = "com.ces.worker.MainKt"
    val dependencies = configurations
        .runtimeClasspath
        .get()
        .map(::zipTree)
    from(dependencies)
        .exclude("META-INF/*.RSA")
        .exclude("META-INF/*.SF")
        .exclude("META-INF/*.DSA")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}