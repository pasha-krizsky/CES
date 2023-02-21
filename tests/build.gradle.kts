val cesVersion: String by project
plugins {
    id("kotlin")
}

group = "com.ces"
version = cesVersion

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("io.kotest:kotest-assertions-core:5.5.5")
    testImplementation("io.kotest:kotest-runner-junit5:5.5.5")
    testImplementation("io.kotest.extensions:kotest-extensions-testcontainers:1.3.4")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}