package com.ces.infrastructure.minio

import io.kotest.core.listeners.AfterSpecListener
import io.kotest.core.listeners.BeforeSpecListener
import io.kotest.core.spec.Spec
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy
import java.time.Duration

class MinioExtension(accessKey: String, secretKey: String) : BeforeSpecListener, AfterSpecListener {

    private val container = GenericContainer(IMAGE)
        .withEnv(ACCESS_KEY_ENV, accessKey)
        .withEnv(SECRET_KEY_ENV, secretKey)
        .withExposedPorts(SERVER_PORT, CONSOLE_PORT)
        .withCommand(COMMAND)

    override suspend fun beforeSpec(spec: Spec) {
        container.setWaitStrategy(
            HttpWaitStrategy()
                .forPath(HEALTH_ENDPOINT)
                .forPort(SERVER_PORT)
                .withStartupTimeout(Duration.ofSeconds(10))
        )
        container.portBindings = listOf("$SERVER_PORT:$SERVER_PORT", "$CONSOLE_PORT:$CONSOLE_PORT")
        container.start()
    }

    override suspend fun afterSpec(spec: Spec) {
        container.stop()
    }

    companion object {
        private const val IMAGE = "minio/minio"
        private const val ACCESS_KEY_ENV = "MINIO_ACCESS_KEY"
        private const val SECRET_KEY_ENV = "MINIO_SECRET_KEY"
        private const val COMMAND = "server /data"

        private const val SERVER_PORT = 9000
        private const val CONSOLE_PORT = 9001

        private const val HEALTH_ENDPOINT = "/minio/health/ready"
    }
}