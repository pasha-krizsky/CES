package com.ces.worker

import com.ces.infrastructure.docker.Docker
import com.ces.worker.config.WorkerConfig
import com.ces.worker.flow.CodeExecutionFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import java.io.File
import java.io.File.separator
import java.io.InputStream

class Bootstrap(
    private val config: WorkerConfig,
    private val docker: Docker,
    private val codeExecutionFlow: CodeExecutionFlow,
) {

    private val log = KotlinLogging.logger {}

    suspend fun start() {
        buildRunnerImage()
        log.debug { "Worker is ready" }
        while (true) {
            codeExecutionFlow.run()
        }
    }

    private suspend fun buildRunnerImage() = withContext(Dispatchers.IO) {
        val dockerfile = loadResource(DOCKERFILE)
        val entrypoint = loadResource(ENTRY_POINT)

        val tmpDockerfile = File(TMP_DIR + separator + DOCKERFILE)
        val tmpEntrypoint = File(TMP_DIR + separator + ENTRY_POINT)

        tmpDockerfile.writeBytes(dockerfile.readAllBytes())
        tmpEntrypoint.writeBytes(entrypoint.readAllBytes())

        docker.buildImage(config.runner.imageName, tmpDockerfile, tmpEntrypoint)

        tmpDockerfile.delete()
        tmpEntrypoint.delete()
    }

    private fun loadResource(name: String): InputStream {
        return this.javaClass.classLoader.getResourceAsStream(name)!!
    }

    companion object {
        private const val DOCKERFILE = "Dockerfile"
        private const val ENTRY_POINT = "entrypoint.sh"

        val TMP_DIR: String = System.getProperty("java.io.tmpdir")
    }
}