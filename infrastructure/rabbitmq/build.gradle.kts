val cesVersion: String by project

val kotlinxCoroutinesVersion: String by project
val kotestVersion: String by project
val kotestExtensionsVersion: String by project
val apacheCommonsTextVersion: String by project
val rabbitmqVersion: String by project
val logbackVersion: String by project
val kotlinLoggingVersion: String by project

plugins {
    id("kotlin")
    id("java-test-fixtures")
}

group = "com.ces"
version = cesVersion

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$kotlinxCoroutinesVersion")
    implementation("com.rabbitmq:amqp-client:$rabbitmqVersion")

    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("io.github.microutils:kotlin-logging-jvm:$kotlinLoggingVersion")

    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-json:$kotestVersion")
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest.extensions:kotest-extensions-testcontainers:$kotestExtensionsVersion")
    testImplementation("org.apache.commons:commons-text:$apacheCommonsTextVersion")

    testFixturesImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testFixturesImplementation("io.kotest:kotest-assertions-json:$kotestVersion")
    testFixturesImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testFixturesImplementation("io.kotest.extensions:kotest-extensions-testcontainers:$kotestExtensionsVersion")
    testFixturesImplementation("org.apache.commons:commons-text:$apacheCommonsTextVersion")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}