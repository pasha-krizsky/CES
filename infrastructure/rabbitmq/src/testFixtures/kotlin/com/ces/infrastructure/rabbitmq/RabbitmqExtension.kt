package com.ces.infrastructure.rabbitmq

import io.kotest.core.listeners.AfterSpecListener
import io.kotest.core.listeners.BeforeSpecListener
import io.kotest.core.spec.Spec
import org.testcontainers.containers.GenericContainer

class RabbitmqExtension : BeforeSpecListener, AfterSpecListener {

    private val container = GenericContainer(IMAGE)
        .withExposedPorts(BROKER_PORT, WEB_PORT)

    override suspend fun beforeSpec(spec: Spec) {
        container.portBindings = listOf("$BROKER_PORT:$BROKER_PORT", "$WEB_PORT:$WEB_PORT")
        container.start()
    }

    override suspend fun afterSpec(spec: Spec) {
        container.stop()
    }

    companion object {
        private const val IMAGE = "rabbitmq:3-management"

        private const val BROKER_PORT = 5672
        private const val WEB_PORT = 15672
    }
}