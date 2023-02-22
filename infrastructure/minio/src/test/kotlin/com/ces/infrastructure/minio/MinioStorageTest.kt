package com.ces.infrastructure.minio

import com.ces.infrastructure.minio.MinioStorageTest.Companion.ACCESS_KEY
import com.ces.infrastructure.minio.MinioStorageTest.Companion.MINIO_ENDPOINT
import com.ces.infrastructure.minio.MinioStorageTest.Companion.SECRET_KEY
import io.kotest.core.spec.style.StringSpec
import io.kotest.engine.spec.tempfile
import io.kotest.matchers.shouldBe
import io.minio.MinioAsyncClient
import org.apache.commons.lang3.RandomStringUtils.randomAlphabetic
import org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric

class MinioStorageTest : StringSpec({

    extension(MinioExtension(ACCESS_KEY, SECRET_KEY))

    val bucketName = randomAlphabetic(BUCKET_NAME_LENGTH).lowercase()
    val sourceFile = tempfile()

    val storage = MinioStorage(minioClient())

    beforeSpec {
        storage.createBucket(bucketName)
    }

    "should upload and download file" {
        val objectPath = sourceFile.name
        val sourceContent = randomAlphanumeric(TEST_FILE_CONTENT_LENGTH)
        sourceFile.writeText(sourceContent)

        storage.uploadFile(bucketName, sourceFile.absolutePath, objectPath)
        val resultPath = sourceFile.absolutePath + DOWNLOADED_SUFFIX
        val resultFile = storage.downloadFile(bucketName, objectPath, resultPath)

        resultFile.readText() shouldBe sourceContent

        resultFile.delete()
    }
}) {
    companion object {
        const val ACCESS_KEY = "minioadmin"
        const val SECRET_KEY = "minioadmin"
        const val MINIO_ENDPOINT = "http://127.0.0.1:9000"
        const val BUCKET_NAME_LENGTH = 10
        const val TEST_FILE_CONTENT_LENGTH = 1000

        const val DOWNLOADED_SUFFIX = "_downloaded"
    }
}

private fun minioClient() = MinioAsyncClient.builder()
    .endpoint(MINIO_ENDPOINT)
    .credentials(ACCESS_KEY, SECRET_KEY)
    .build()
