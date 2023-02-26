val cesVersion: String by project

val kotlinxCoroutinesVersion: String by project
val kotlinxSerializationVersion: String by project
val kotlinxDatetimeVersion: String by project
val kotestVersion: String by project
val kotestExtensionsVersion: String by project
val apacheCommonsCompressVersion: String by project
val minioVersion: String by project

plugins {
    id("kotlin")
    kotlin("plugin.serialization") version "1.8.10"
    id("java-test-fixtures")
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

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$kotlinxCoroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:$kotlinxDatetimeVersion")

    implementation("org.apache.commons:commons-compress:$apacheCommonsCompressVersion")
    implementation("io.minio:minio:$minioVersion")

    testImplementation(testFixtures(project(":infrastructure:minio")))
    testImplementation(testFixtures(project(":infrastructure:rabbitmq")))
    testImplementation(testFixtures(project(":infrastructure:docker")))

    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-json:$kotestVersion")
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest.extensions:kotest-extensions-testcontainers:$kotestExtensionsVersion")

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