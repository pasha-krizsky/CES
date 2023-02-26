val cesVersion: String by project

val kotestVersion: String by project
val kotestExtensionsVersion: String by project

plugins {
    id("kotlin")
}

group = "com.ces"
version = cesVersion

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest.extensions:kotest-extensions-testcontainers:$kotestExtensionsVersion")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}