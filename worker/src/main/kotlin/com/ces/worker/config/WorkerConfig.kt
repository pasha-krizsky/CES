package com.ces.worker.config

import com.ces.infrastructure.minio.MinioConfig
import com.ces.infrastructure.rabbitmq.config.QueueConfig
import com.ces.infrastructure.rabbitmq.config.RabbitmqConfig

class WorkerConfig(
    val docker: DockerConfig,
    val runner: RunnerConfig,
    val broker: RabbitmqConfig,
    val codeExecutionRequestQueue: QueueConfig,
    val codeExecutionResponseQueue: QueueConfig,
    val minio: MinioConfig,
    val bucketName: String,
)