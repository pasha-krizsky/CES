val cesVersion: String by project
val kotlinExposedVersion: String by project
val kotestVersion: String by project
val kotestExtensionsVersion: String by project

val postgresVersion: String by project
val hikariVersion: String by project

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
    api("org.jetbrains.exposed:exposed-core:$kotlinExposedVersion")
    api("org.jetbrains.exposed:exposed-kotlin-datetime:$kotlinExposedVersion")
    api("org.postgresql:postgresql:$postgresVersion")
    api("com.zaxxer:HikariCP:$hikariVersion")

    runtimeOnly("org.jetbrains.exposed:exposed-jdbc:$kotlinExposedVersion")

    testFixturesImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testFixturesImplementation("io.kotest:kotest-assertions-json:$kotestVersion")
    testFixturesImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testFixturesImplementation("io.kotest.extensions:kotest-extensions-testcontainers:$kotestExtensionsVersion")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}