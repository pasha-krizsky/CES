package com.ces.infrastructure.docker

class DockerConfig {

    companion object {
        val socket: String
            get() = with(OS_NAME) {
                when {
                    contains(WINDOWS, ignoreCase = true) -> DOCKER_HOST_WINDOWS
                    else -> DOCKER_HOST_UNIX
                }
            }

        private val OS_NAME: String = System.getProperty("os.name")
        private const val WINDOWS = "windows"

        private const val DOCKER_HOST_WINDOWS = "npipe:////./pipe/docker_engine"
        private const val DOCKER_HOST_UNIX = "unix:///var/run/docker.sock"
    }
}