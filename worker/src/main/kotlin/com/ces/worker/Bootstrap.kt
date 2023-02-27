package com.ces.worker

import com.ces.infrastructure.docker.Docker
import com.ces.worker.config.WorkerConfig
import com.ces.worker.flow.CodeExecutionFlow
import java.io.File

class Bootstrap(
    private val config: WorkerConfig,
    private val docker: Docker,
    private val codeExecutionFlow: CodeExecutionFlow,
) {
    suspend fun start() {
        buildRunnerImage()
        while (true) {
            codeExecutionFlow.run()
        }
    }

    private suspend fun buildRunnerImage() {
        val dockerfile = loadResource(DOCKERFILE)
        val entrypoint = loadResource(ENTRY_POINT)

        docker.buildImage(config.runner.imageName, dockerfile, entrypoint)
    }

    private fun loadResource(name: String): File {
        val resource = this::class.java.getResource("/$name")!!.file
        return File(resource)
    }

    companion object {
        private const val DOCKERFILE = "Dockerfile"
        private const val ENTRY_POINT = "entrypoint.sh"
    }
}