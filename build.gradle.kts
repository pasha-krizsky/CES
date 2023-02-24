val cesVersion: String by project
val kotlinVersion: String by project

plugins {
    kotlin("jvm") version "1.8.10"
}

group = "com.ces"
version = cesVersion

repositories {
    mavenCentral()
}