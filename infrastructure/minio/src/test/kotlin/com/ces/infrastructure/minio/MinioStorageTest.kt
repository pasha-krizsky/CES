package com.ces.infrastructure.minio

import com.ces.infrastructure.minio.MinioTestData.Companion.MINIO_ACCESS_KEY
import com.ces.infrastructure.minio.MinioTestData.Companion.MINIO_CODE_EXECUTION_BUCKET_NAME
import com.ces.infrastructure.minio.MinioTestData.Companion.MINIO_ENDPOINT
import com.ces.infrastructure.minio.MinioTestData.Companion.MINIO_SECRET_KEY
import io.kotest.core.spec.style.StringSpec
import io.kotest.engine.spec.tempfile
import io.kotest.matchers.shouldBe
import io.minio.MinioAsyncClient
import org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric

class MinioStorageTest : StringSpec({

    extension(MinioExtension(MINIO_ACCESS_KEY, MINIO_SECRET_KEY))

    val sourceFile = tempfile()

    val storage = MinioStorage(minioClient())

    beforeSpec {
        storage.createBucket(MINIO_CODE_EXECUTION_BUCKET_NAME)
    }

    "should upload and download file" {
        val objectPath = sourceFile.name
        val sourceContent = randomAlphanumeric(TEST_FILE_CONTENT_LENGTH)
        sourceFile.writeText(sourceContent)

        storage.uploadFile(MINIO_CODE_EXECUTION_BUCKET_NAME, sourceFile.absolutePath, objectPath)
        val resultPath = sourceFile.absolutePath + DOWNLOADED_SUFFIX
        val resultFile = storage.downloadFile(MINIO_CODE_EXECUTION_BUCKET_NAME, objectPath, resultPath)

        resultFile.readText() shouldBe sourceContent

        resultFile.delete()
    }
}) {
    companion object {
        const val TEST_FILE_CONTENT_LENGTH = 1000
        const val DOWNLOADED_SUFFIX = "_downloaded"
    }
}

private fun minioClient() = MinioAsyncClient.builder()
    .endpoint(MINIO_ENDPOINT)
    .credentials(MINIO_ACCESS_KEY, MINIO_SECRET_KEY)
    .build()
