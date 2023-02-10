val cesVersion: String by project
val kotlinVersion: String by project

plugins {
    id("kotlin")
}

group = "ces"
version = cesVersion

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
}