package com.ces.infrastructure.minio

import io.minio.*
import kotlinx.coroutines.future.await
import java.io.File

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

    override suspend fun uploadFile(bucketName: String, localSource: String, storageDestination: String) {
        minioClient.uploadObject(
            UploadObjectArgs.builder()
                .bucket(bucketName)
                .`object`(storageDestination)
                .filename(localSource)
                .build()
        ).await()
    }

    override suspend fun downloadFile(bucketName: String, storageSource: String, localDestination: String): File {
        minioClient.downloadObject(
            DownloadObjectArgs.builder()
                .bucket(bucketName)
                .`object`(storageSource)
                .filename(localDestination)
                .build()
        ).await()
        return File(localDestination)
    }
}