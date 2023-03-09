package com.ces.infrastructure.minio

import com.ces.infrastructure.minio.MinioTestData.Companion.MINIO_ACCESS_KEY
import com.ces.infrastructure.minio.MinioTestData.Companion.MINIO_CODE_EXECUTION_BUCKET_NAME
import com.ces.infrastructure.minio.MinioTestData.Companion.MINIO_ENDPOINT
import com.ces.infrastructure.minio.MinioTestData.Companion.MINIO_SECRET_KEY
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.engine.spec.tempfile
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.minio.MinioAsyncClient
import org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric

class MinioStorageTest : StringSpec({

    extension(MinioExtension(MINIO_ACCESS_KEY, MINIO_SECRET_KEY))

    val sourceFile = tempfile()

    val storage = MinioStorage(minioClient())

    beforeSpec {
        storage.createBucket(MINIO_CODE_EXECUTION_BUCKET_NAME)
    }

    "should upload and get" {
        val objectPath = sourceFile.name
        val sourceContent = randomAlphanumeric(TEST_FILE_CONTENT_LENGTH)
        sourceFile.writeText(sourceContent)

        storage.upload(MINIO_CODE_EXECUTION_BUCKET_NAME, sourceFile.absolutePath, objectPath)
        val resultPath = sourceFile.absolutePath + DOWNLOADED_SUFFIX
        val resultFile = storage.get(MINIO_CODE_EXECUTION_BUCKET_NAME, objectPath, resultPath)

        resultFile.readText() shouldBe sourceContent

        resultFile.delete()
    }

    "should upload and find" {
        val objectPath = sourceFile.name
        val sourceContent = randomAlphanumeric(TEST_FILE_CONTENT_LENGTH)
        sourceFile.writeText(sourceContent)

        storage.upload(MINIO_CODE_EXECUTION_BUCKET_NAME, sourceFile.absolutePath, objectPath)
        val resultPath = sourceFile.absolutePath + DOWNLOADED_SUFFIX
        val resultFile = storage.find(MINIO_CODE_EXECUTION_BUCKET_NAME, objectPath, resultPath)

        resultFile shouldNotBe null
        resultFile?.readText() shouldBe sourceContent
        resultFile?.delete()
    }

    "should fail to get when object not found" {
        val objectPath = randomAlphanumeric(10)
        val exception = shouldThrow<IllegalArgumentException> {
            storage.get(MINIO_CODE_EXECUTION_BUCKET_NAME, objectPath, "")
        }
        exception.message shouldBe "Object not found: bucket=$MINIO_CODE_EXECUTION_BUCKET_NAME, objectPath=$objectPath"
    }

    "should return null when object not found" {
        val objectPath = randomAlphanumeric(10)
        storage.find(MINIO_CODE_EXECUTION_BUCKET_NAME, objectPath, "") shouldBe null
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
