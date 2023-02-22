plugins {
    id("kotlin")
    id ("java-test-fixtures")
}

group = "com.ces"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.6.4")
    implementation("com.rabbitmq:amqp-client:5.16.0")

    testImplementation("io.kotest:kotest-assertions-core:5.5.5")
    testImplementation("io.kotest:kotest-assertions-json:5.5.5")
    testImplementation("io.kotest:kotest-runner-junit5:5.5.5")
    testImplementation("io.kotest.extensions:kotest-extensions-testcontainers:1.3.4")
    testImplementation("org.apache.commons:commons-text:1.10.0")

    testFixturesImplementation("io.kotest:kotest-assertions-core:5.5.5")
    testFixturesImplementation("io.kotest:kotest-assertions-json:5.5.5")
    testFixturesImplementation("io.kotest:kotest-runner-junit5:5.5.5")
    testFixturesImplementation("io.kotest.extensions:kotest-extensions-testcontainers:1.3.4")
    testFixturesImplementation("org.apache.commons:commons-text:1.10.0")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}