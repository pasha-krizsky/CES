plugins {
    id("kotlin")
}

group = "com.ces"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    api("com.github.docker-java:docker-java:3.2.14")
    api("com.github.docker-java:docker-java-transport-httpclient5:3.2.14")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")

    testImplementation("io.kotest:kotest-assertions-core:5.5.5")
    testImplementation("io.kotest:kotest-assertions-json:5.5.5")
    testImplementation("io.kotest:kotest-runner-junit5:5.5.5")
    testImplementation("io.kotest.extensions:kotest-extensions-testcontainers:1.3.4")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}