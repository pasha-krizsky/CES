package com.ces.infrastructure.minio

import io.minio.*
import io.minio.errors.ErrorResponseException
import kotlinx.coroutines.future.await
import java.io.File
import java.util.concurrent.CompletionException

private const val NO_SUCH_KEY_ERROR = "NoSuchKey"

class MinioStorage(
    private val minioClient: MinioAsyncClient,
) : ObjectStorage {

    override suspend fun createBucket(bucketName: String) {
        val exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build()).await()
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build()).await()
        } else {
            println("Bucket '$bucketName' already exists")
        }
    }

    override suspend fun upload(bucketName: String, fromPath: String, toPath: String) {
        minioClient.uploadObject(
            UploadObjectArgs.builder()
                .bucket(bucketName)
                .`object`(toPath)
                .filename(fromPath)
                .build()
        ).await()
    }

    override suspend fun get(bucketName: String, fromPath: String, toPath: String): File {
        return find(bucketName, fromPath, toPath)
            ?: throw IllegalArgumentException("Object not found: bucket=$bucketName, objectPath=$fromPath")
    }

    override suspend fun find(bucketName: String, fromPath: String, toPath: String): File? {
        if (!objectExists(bucketName, fromPath))
            return null

        minioClient.downloadObject(
            DownloadObjectArgs.builder()
                .bucket(bucketName)
                .`object`(fromPath)
                .filename(toPath)
                .build()
        ).await()
        return File(toPath)
    }

    private suspend fun objectExists(bucketName: String, objectPath: String): Boolean {
        try {
            minioClient.statObject(
                StatObjectArgs.builder()
                    .bucket(bucketName)
                    .`object`(objectPath)
                    .build()
            ).await()
        } catch (e: CompletionException) {
            val cause = e.cause
            if (cause !is ErrorResponseException)
                return false
            return cause.errorResponse()?.code() == NO_SUCH_KEY_ERROR
        }
        return true
    }
}