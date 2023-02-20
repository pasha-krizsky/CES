package com.ces.worker.config

class RunnerConfig(
    val imageName: String,
    val workDir: String,
    val codeExecutionTimeoutMillis: Long,
    val logsPollIntervalMillis: Long,
    val container: RunnerContainerConfig,
)

class RunnerContainerConfig(
    val capDrop: String,
    val cgroupnsMode: String,
    val networkMode: String,
    val cpusetCpus: String,
    val cpuQuota: Long,
    val memory: Long,
    val memorySwap: Long,
)