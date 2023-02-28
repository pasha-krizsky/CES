package com.ces.worker.config

import com.ces.infrastructure.docker.DockerTestData.Companion.CODE_EXECUTION_TIMEOUT_MILLIS
import com.ces.infrastructure.docker.DockerTestData.Companion.LOGS_POLL_INTERVAL_MILLIS
import com.ces.infrastructure.docker.DockerTestData.Companion.RUNNER_CAP_DROP
import com.ces.infrastructure.docker.DockerTestData.Companion.RUNNER_CGROUPNS_MODE
import com.ces.infrastructure.docker.DockerTestData.Companion.RUNNER_CPUSET_CPUS
import com.ces.infrastructure.docker.DockerTestData.Companion.RUNNER_CPU_QUOTA
import com.ces.infrastructure.docker.DockerTestData.Companion.RUNNER_HOME_DIR
import com.ces.infrastructure.docker.DockerTestData.Companion.RUNNER_IPC_MODE
import com.ces.infrastructure.docker.DockerTestData.Companion.RUNNER_KERNEL_MEMORY
import com.ces.infrastructure.docker.DockerTestData.Companion.RUNNER_MEMORY
import com.ces.infrastructure.docker.DockerTestData.Companion.RUNNER_MEMORY_SWAP
import com.ces.infrastructure.docker.DockerTestData.Companion.RUNNER_NETWORK_MODE
import com.ces.infrastructure.docker.DockerTestData.Companion.RUNNER_NOFILE_HARD
import com.ces.infrastructure.docker.DockerTestData.Companion.RUNNER_NOFILE_SOFT
import com.ces.infrastructure.docker.DockerTestData.Companion.RUNNER_NPROC_HARD
import com.ces.infrastructure.docker.DockerTestData.Companion.RUNNER_NPROC_SOFT
import com.ces.infrastructure.docker.DockerTestData.Companion.RUNNER_PIDS_LIMIT
import com.ces.infrastructure.docker.DockerTestData.Companion.RUNNER_TEST_IMAGE_NAME
import com.ces.infrastructure.minio.MinioConfig
import com.ces.infrastructure.minio.MinioTestData.Companion.MINIO_ACCESS_KEY
import com.ces.infrastructure.minio.MinioTestData.Companion.MINIO_CODE_EXECUTION_BUCKET_NAME
import com.ces.infrastructure.minio.MinioTestData.Companion.MINIO_ENDPOINT
import com.ces.infrastructure.minio.MinioTestData.Companion.MINIO_SECRET_KEY
import com.ces.infrastructure.rabbitmq.RabbitmqTestData.Companion.CODE_EXECUTION_REQUEST_QUEUE_NAME
import com.ces.infrastructure.rabbitmq.RabbitmqTestData.Companion.CODE_EXECUTION_REQUEST_QUEUE_PREFETCH
import com.ces.infrastructure.rabbitmq.RabbitmqTestData.Companion.CODE_EXECUTION_RESPONSE_QUEUE_NAME
import com.ces.infrastructure.rabbitmq.RabbitmqTestData.Companion.CODE_EXECUTION_RESPONSE_QUEUE_PREFETCH
import com.ces.infrastructure.rabbitmq.RabbitmqTestData.Companion.RABBIT_MQ_HOST
import com.ces.infrastructure.rabbitmq.RabbitmqTestData.Companion.RABBIT_MQ_PASSWORD
import com.ces.infrastructure.rabbitmq.RabbitmqTestData.Companion.RABBIT_MQ_PORT
import com.ces.infrastructure.rabbitmq.RabbitmqTestData.Companion.RABBIT_MQ_USER
import com.ces.infrastructure.rabbitmq.config.QueueConfig
import com.ces.infrastructure.rabbitmq.config.RabbitmqConfig

class ApplicationConfigTestData {

    companion object {
        fun applicationConfig() = WorkerConfig(
            runner = RunnerConfig(
                imageName = RUNNER_TEST_IMAGE_NAME,
                workDir = RUNNER_HOME_DIR,
                codeExecutionTimeoutMillis = CODE_EXECUTION_TIMEOUT_MILLIS,
                logsPollIntervalMillis = LOGS_POLL_INTERVAL_MILLIS,
                container = RunnerContainerConfig(
                    capDrop = RUNNER_CAP_DROP,
                    cgroupnsMode = RUNNER_CGROUPNS_MODE,
                    networkMode = RUNNER_NETWORK_MODE,
                    cpusetCpus = RUNNER_CPUSET_CPUS,
                    cpuQuota = RUNNER_CPU_QUOTA,
                    memory = RUNNER_MEMORY,
                    memorySwap = RUNNER_MEMORY_SWAP,
                    kernelMemoryTcpBytes = RUNNER_KERNEL_MEMORY,
                    pidsLimit = RUNNER_PIDS_LIMIT,
                    ipcMode = RUNNER_IPC_MODE,
                    nofileSoft = RUNNER_NOFILE_SOFT,
                    nofileHard = RUNNER_NOFILE_HARD,
                    nprocSoft = RUNNER_NPROC_SOFT,
                    nprocHard = RUNNER_NPROC_HARD,
                )
            ),
            codeExecutionBucketName = MINIO_CODE_EXECUTION_BUCKET_NAME,
            rabbitmq = RabbitmqConfig(
                user = RABBIT_MQ_USER,
                password = RABBIT_MQ_PASSWORD,
                host = RABBIT_MQ_HOST,
                port = RABBIT_MQ_PORT,
            ),
            codeExecutionRequestQueue = QueueConfig(
                name = CODE_EXECUTION_REQUEST_QUEUE_NAME,
                prefetchCount = CODE_EXECUTION_REQUEST_QUEUE_PREFETCH,
            ),
            codeExecutionResponseQueue = QueueConfig(
                name = CODE_EXECUTION_RESPONSE_QUEUE_NAME,
                prefetchCount = CODE_EXECUTION_RESPONSE_QUEUE_PREFETCH,
            ),
            minio = MinioConfig(
                endpoint = MINIO_ENDPOINT,
                accessKey = MINIO_ACCESS_KEY,
                secretKey = MINIO_SECRET_KEY,
            )
        )
    }
}