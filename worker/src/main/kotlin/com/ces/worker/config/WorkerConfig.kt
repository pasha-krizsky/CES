package com.ces.worker.config

import com.ces.infrastructure.minio.MinioConfig
import com.ces.infrastructure.rabbitmq.config.QueueConfig
import com.ces.infrastructure.rabbitmq.config.RabbitmqConfig
import com.typesafe.config.Config

class WorkerConfig(
    val maxConcurrentExecutions: Int,
    val runner: RunnerConfig,
    val rabbitmq: RabbitmqConfig,
    val codeExecutionRequestQueue: QueueConfig,
    val codeExecutionResponseQueue: QueueConfig,
    val minio: MinioConfig,
    val codeExecutionBucketName: String,
) {
    companion object {

        val tmpDir: String = System.getProperty("java.io.tmpdir")

        fun from(config: Config): WorkerConfig {
            val runner = RunnerConfig(
                imageName = stringProperty(config, RUNNER_IMAGE_NAME),
                workDir = stringProperty(config, RUNNER_WORK_DIR),
                codeExecutionTimeoutMillis = longProperty(config, RUNNER_CODE_EXECUTION_TIMEOUT),
                logsPollIntervalMillis = longProperty(config, RUNNER_LOGS_POLL_INTERVAL),
                container = RunnerContainerConfig(
                    capDrop = stringProperty(config, CAP_DROP),
                    cgroupnsMode = stringProperty(config, CGROUPNS_MODE),
                    networkMode = stringProperty(config, NETWORK_MODE),
                    cpusetCpus = stringProperty(config, CPUSET_CPUS),
                    cpuQuota = longProperty(config, CPU_QUOTA),
                    memory = longProperty(config, MEMORY),
                    memorySwap = longProperty(config, MEMORY_SWAP),
                    kernelMemoryTcpBytes = longProperty(config, KERNEL_MEMORY_TCP),
                    pidsLimit = intProperty(config, PIDS_LIMIT),
                    ipcMode = stringProperty(config, IPC_MODE),
                    nofileSoft = intProperty(config, NOFILE_SOFT),
                    nofileHard = intProperty(config, NOFILE_HARD),
                    nprocSoft = intProperty(config, NPROC_SOFT),
                    nprocHard = intProperty(config, NPROC_HARD),
                )
            )
            val rabbitmq = RabbitmqConfig(
                user = stringProperty(config, RABBITMQ_USER),
                password = stringProperty(config, RABBITMQ_PASSWORD),
                host = stringProperty(config, RABBITMQ_HOST),
                port = intProperty(config, RABBITMQ_PORT),
            )
            val codeExecutionRequestQueue = QueueConfig(
                stringProperty(config, CODE_EXECUTION_REQUEST_QUEUE_NAME),
                intProperty(config, CODE_EXECUTION_REQUEST_QUEUE_PREFETCH),
            )
            val codeExecutionResponseQueue = QueueConfig(
                stringProperty(config, CODE_EXECUTION_RESPONSE_QUEUE_NAME),
                intProperty(config, CODE_EXECUTION_RESPONSE_QUEUE_PREFETCH),
            )
            val minio = MinioConfig(
                stringProperty(config, MINIO_ENDPOINT),
                stringProperty(config, MINIO_ACCESS_KEY),
                stringProperty(config, MINIO_SECRET_KEY),
            )

            return WorkerConfig(
                maxConcurrentExecutions = intProperty(config, MAX_CONCURRENT_EXECUTIONS),
                rabbitmq = rabbitmq,
                codeExecutionRequestQueue = codeExecutionRequestQueue,
                codeExecutionResponseQueue = codeExecutionResponseQueue,
                minio = minio,
                codeExecutionBucketName = stringProperty(config, BUCKET_NAME),
                runner = runner,
            )
        }

        private fun stringProperty(hocon: Config, name: String) = hocon.getString(name)
        private fun intProperty(hocon: Config, name: String) = stringProperty(hocon, name).toInt()
        private fun longProperty(hocon: Config, name: String) = stringProperty(hocon, name).toLong()

        private const val MAX_CONCURRENT_EXECUTIONS = "maxConcurrentExecutions"

        private const val RUNNER_IMAGE_NAME = "runner.imageName"
        private const val RUNNER_WORK_DIR = "runner.workDir"
        private const val RUNNER_CODE_EXECUTION_TIMEOUT = "runner.codeExecutionTimeoutMillis"
        private const val RUNNER_LOGS_POLL_INTERVAL = "runner.logsPollIntervalMillis"

        private const val CAP_DROP = "runner.container.capDrop"
        private const val CGROUPNS_MODE = "runner.container.cgroupnsMode"
        private const val NETWORK_MODE = "runner.container.networkMode"
        private const val CPUSET_CPUS = "runner.container.cpusetCpus"
        private const val CPU_QUOTA = "runner.container.cpuQuota"
        private const val MEMORY = "runner.container.memory"
        private const val MEMORY_SWAP = "runner.container.memorySwap"
        private const val KERNEL_MEMORY_TCP = "runner.container.kernelMemoryTcp"
        private const val PIDS_LIMIT = "runner.container.pidsLimit"
        private const val IPC_MODE = "runner.container.ipcMode"
        private const val NOFILE_SOFT = "runner.container.nofileSoft"
        private const val NOFILE_HARD = "runner.container.nofileHard"
        private const val NPROC_SOFT = "runner.container.nprocSoft"
        private const val NPROC_HARD = "runner.container.nprocHard"

        private const val BUCKET_NAME = "codeExecutionBucketName"

        private const val RABBITMQ_USER = "rabbitmq.user"
        private const val RABBITMQ_PASSWORD = "rabbitmq.password"
        private const val RABBITMQ_HOST = "rabbitmq.host"
        private const val RABBITMQ_PORT = "rabbitmq.port"

        private const val CODE_EXECUTION_REQUEST_QUEUE_NAME = "codeExecutionRequestQueue.name"
        private const val CODE_EXECUTION_REQUEST_QUEUE_PREFETCH = "codeExecutionRequestQueue.prefetchCount"

        private const val CODE_EXECUTION_RESPONSE_QUEUE_NAME = "codeExecutionResponseQueue.name"
        private const val CODE_EXECUTION_RESPONSE_QUEUE_PREFETCH = "codeExecutionResponseQueue.prefetchCount"

        private const val MINIO_ENDPOINT = "minio.endpoint"
        private const val MINIO_ACCESS_KEY = "minio.accessKey"
        private const val MINIO_SECRET_KEY = "minio.secretKey"
    }
}