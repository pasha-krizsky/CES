val cesVersion: String by project

val kotlinxCoroutinesVersion: String by project
val kotlinxSerializationVersion: String by project
val kotlinxDatetimeVersion: String by project
val kotestVersion: String by project
val kotestExtensionsVersion: String by project
val apacheCommonsTextVersion: String by project
val minioVersion: String by project

plugins {
    id("kotlin")
    kotlin("plugin.serialization") version "1.8.10"
    id("java-test-fixtures")
}

group = "com.ces"
version = cesVersion

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:$kotlinxDatetimeVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")

    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-json:$kotestVersion")
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("org.apache.commons:commons-text:$apacheCommonsTextVersion")

    testFixturesImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")
    testFixturesImplementation("org.jetbrains.kotlinx:kotlinx-datetime:$kotlinxDatetimeVersion")
    testFixturesImplementation("org.apache.commons:commons-text:$apacheCommonsTextVersion")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}