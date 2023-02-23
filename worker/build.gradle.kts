val cesVersion: String by project
val kotlinVersion: String by project
val ktorVersion: String by project
val logbackVersion: String by project
val junitVersion: String by project

plugins {
    id("kotlin")
    id("io.ktor.plugin") version "2.2.3"
    kotlin("plugin.serialization") version "1.8.10"
    id ("java-test-fixtures")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

group = "com.ces"
version = cesVersion

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":infrastructure:docker"))
    implementation(project(":infrastructure:rabbitmq"))
    implementation(project(":infrastructure:minio"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.6.4")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
    implementation("org.apache.commons:commons-compress:1.22")
    implementation("io.minio:minio:8.5.2")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")

    testImplementation("io.kotest:kotest-assertions-core:5.5.5")
    testImplementation("io.kotest:kotest-assertions-json:5.5.5")
    testImplementation("io.kotest:kotest-runner-junit5:5.5.5")
    testImplementation("io.kotest.extensions:kotest-extensions-testcontainers:1.3.4")

    testImplementation(testFixtures(project(":infrastructure:minio")))
    testImplementation(testFixtures(project(":infrastructure:rabbitmq")))
    testImplementation(testFixtures(project(":infrastructure:docker")))

    testFixturesImplementation(testFixtures(project(":infrastructure:minio")))
    testFixturesImplementation(testFixtures(project(":infrastructure:rabbitmq")))
    testFixturesImplementation(testFixtures(project(":infrastructure:docker")))
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