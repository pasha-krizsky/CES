plugins {
    id("kotlin")
}

group = "com.ces"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.minio:minio:8.5.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.6.4")

    testImplementation("io.kotest:kotest-assertions-core:5.5.5")
    testImplementation("io.kotest:kotest-assertions-json:5.5.5")
    testImplementation("io.kotest:kotest-runner-junit5:5.5.5")
    testImplementation("io.kotest.extensions:kotest-extensions-testcontainers:1.3.4")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}