val cesVersion: String by project
plugins {
    kotlin("jvm") version "1.8.0"
    application
}

group = "com.ces"
version = cesVersion

repositories {
    mavenCentral()
}