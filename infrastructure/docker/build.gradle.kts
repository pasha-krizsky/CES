val cesVersion: String by project

val kotlinxCoroutinesVersion: String by project
val kotlinxSerializationVersion: String by project
val kotestVersion: String by project
val kotestExtensionsVersion: String by project
val dockerJavaClientVersion: String by project
val logbackVersion: String by project
val kotlinLoggingVersion: String by project

plugins {
    id("kotlin")
    id ("java-test-fixtures")
}

group = "com.ces"
version = cesVersion

repositories {
    mavenCentral()
}

dependencies {
    api("com.github.docker-java:docker-java:$dockerJavaClientVersion")
    api("com.github.docker-java:docker-java-transport-httpclient5:$dockerJavaClientVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$kotlinxCoroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")

    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("io.github.microutils:kotlin-logging-jvm:$kotlinLoggingVersion")

    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-json:$kotestVersion")
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest.extensions:kotest-extensions-testcontainers:$kotestExtensionsVersion")
    
    testFixturesImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}