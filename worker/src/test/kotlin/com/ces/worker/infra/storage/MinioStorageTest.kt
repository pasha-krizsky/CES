package com.ces.worker.infra.storage

import io.kotest.core.extensions.install
import io.kotest.core.spec.style.StringSpec
import io.kotest.engine.spec.tempfile
import io.kotest.extensions.testcontainers.TestContainerExtension
import io.kotest.matchers.shouldBe
import io.minio.MinioAsyncClient
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy
import java.io.File
import java.time.Duration

class MinioStorageTest : StringSpec({

    val imageName = "minio/minio"

    val minioEndpoint = "http://127.0.0.1:9000"
    val accessKey = "minioadmin"
    val secretKey = "minioadmin"

    val sourceFile = tempfile()

    // run -p 9000:9000 -p 9001:9001 quay.io/minio/minio server /data --console-address ":9001"
    install(TestContainerExtension(GenericContainer(imageName))) {
        withEnv("MINIO_ACCESS_KEY", accessKey)
        withEnv("MINIO_SECRET_KEY", secretKey)
        portBindings = listOf("9000:9000", "9001:9001")
        withExposedPorts(9000, 9001)
        withCommand("server /data")
        waitingFor(
            HttpWaitStrategy()
                .forPath("/minio/health/ready")
                .forPort(9000)
                .withStartupTimeout(Duration.ofSeconds(10))
        )
    }

    "should upload and download file" {
        val bucketName = "test-bucket"
        val objectPath = "test/${sourceFile.name}"
        val sourceFileContent = "test content"
        sourceFile.writeText(sourceFileContent)

        val minioClient: MinioAsyncClient = MinioAsyncClient.builder()
            .endpoint(minioEndpoint)
            .credentials(accessKey, secretKey)
            .build()
        val minioStorage = MinioStorage(minioClient)
        minioStorage.createBucket(bucketName)
        minioStorage.uploadFile(bucketName, sourceFile.absolutePath, objectPath)
        val resultPath = sourceFile.absolutePath + "-result"
        minioStorage.downloadFile(bucketName, objectPath, resultPath)
        val resultFile = File(resultPath)

        resultFile.readText() shouldBe sourceFileContent

        resultFile.delete()
    }
})
