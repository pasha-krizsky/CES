package com.ces.infrastructure.database

import io.kotest.core.listeners.AfterSpecListener
import io.kotest.core.listeners.BeforeSpecListener
import io.kotest.core.spec.Spec
import org.testcontainers.containers.GenericContainer

class DatabaseExtension(config: DatabaseConfig) : BeforeSpecListener, AfterSpecListener {

    private val container = GenericContainer(IMAGE)
        .withEnv(POSTGRES_USER_ENV, config.user)
        .withEnv(POSTGRES_PASSWORD_ENV, config.password)
        .withExposedPorts(PORT, PORT)

    override suspend fun beforeSpec(spec: Spec) {
        container.portBindings = listOf("$PORT:$PORT")
        container.start()
    }

    override suspend fun afterSpec(spec: Spec) {
        container.stop()
    }

    companion object {
        private const val IMAGE = "postgres"

        private const val POSTGRES_USER_ENV = "POSTGRES_USER"
        private const val POSTGRES_PASSWORD_ENV = "POSTGRES_PASSWORD"

        private const val PORT = 5432
    }
}