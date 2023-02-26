package com.ces.server

import com.ces.infrastructure.minio.ObjectStorage
import com.ces.server.config.ServerConfig
import com.ces.server.module.appModule
import com.ces.server.plugins.configureRouting
import com.ces.server.plugins.configureSerialization
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun main(args: Array<String>): Unit =
    io.ktor.server.cio.EngineMain.main(args)

@Suppress("unused")
fun Application.module() = runBlocking {
    install(Koin) {
        slf4jLogger()
        modules(appModule)
    }

    val storage by inject<ObjectStorage>()
    val config by inject<ServerConfig>()

    storage.createBucket(config.codeExecutionBucketName)

    configureSerialization()
    configureRouting()
}