package com.ces.worker.config

class ApplicationConfig(
    val docker: DockerConfig,
    val runner: RunnerConfig,
    val broker: MessageBrokerConfig,
    val codeExecutionRequestQueue: QueueConfig,
    val codeExecutionResponseQueue: QueueConfig,
    val minio: MinioConfig,
    val bucketName: String,
    val localStoragePath: String,
)