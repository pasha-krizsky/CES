plugins {
    id("kotlin")
    kotlin("plugin.serialization") version "1.8.10"
}

group = "com.ces"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")

    testImplementation("io.kotest:kotest-assertions-core:5.5.5")
    testImplementation("io.kotest:kotest-assertions-json:5.5.5")
    testImplementation("io.kotest:kotest-runner-junit5:5.5.5")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}