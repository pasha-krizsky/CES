package com.ces.worker.infra.storage

import io.minio.*
import kotlinx.coroutines.future.await

class MinioStorage : ObjectStorage {

    private val minioClient: MinioAsyncClient = MinioAsyncClient.builder()
        .endpoint(MIN_IO_ENDPOINT)
        .credentials(ACCESS_KEY, SECRET_KEY)
        .build()

    override suspend fun createBucket(bucketName: String) {
        val exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build()).await()
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build()).await()
        } else {
            println("Bucket '$bucketName' already exists")
        }
    }

    override suspend fun uploadFile(bucketName: String, localSource: String, storageDestination: String) {
        println("Uploading...")
        minioClient.uploadObject(
            UploadObjectArgs.builder()
                .bucket(bucketName)
                .`object`(storageDestination)
                .filename(localSource)
                .build()
        ).await()
        println("Uploaded")
    }

    override suspend fun downloadFile(bucketName: String, storageSource: String, localDestination: String) {
        println("Downloading...")
        minioClient.downloadObject(
            DownloadObjectArgs.builder()
                .bucket(bucketName)
                .`object`(storageSource)
                .filename(localDestination)
                .build()
        ).await()
        println("Downloaded")
    }
}