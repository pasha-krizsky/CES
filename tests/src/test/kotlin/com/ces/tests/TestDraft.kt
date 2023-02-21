package com.ces.tests

import io.kotest.core.extensions.install
import io.kotest.core.spec.style.StringSpec
import io.kotest.extensions.testcontainers.TestContainerExtension
import kotlinx.coroutines.delay
import org.testcontainers.containers.BindMode.READ_ONLY
import org.testcontainers.containers.GenericContainer
import org.testcontainers.images.builder.ImageFromDockerfile
import java.nio.file.Path

//const val WORKER_DOCKER_FILE_PATH = "E:\\projects\\CES\\worker\\Dockerfile"
//const val DOCKER_SOCKET_HOST = "//var/run/docker.sock"
//const val DOCKER_SOCKET_WORKER = "/var/run/docker.sock"


// Now to start the tests it's needed to build jar first:
// ./gradlew clean jar
// Also, runner image should be built in advance:
// docker build -t mono-runner .
//
// It should be automated
//class HelloWorldTest : StringSpec({

//    val workerContainer = install(
//        TestContainerExtension(
//            GenericContainer(ImageFromDockerfile().withDockerfile(Path.of(WORKER_DOCKER_FILE_PATH)))
//        )
//    ) {
//        withFileSystemBind(DOCKER_SOCKET_HOST, DOCKER_SOCKET_WORKER, READ_ONLY)
//        startupAttempts = 1
//    }

//    "should execute code and return results" {
//        delay(7000)
//        println("Container logs:")
//        println(workerContainer.logs)
//    }
//})