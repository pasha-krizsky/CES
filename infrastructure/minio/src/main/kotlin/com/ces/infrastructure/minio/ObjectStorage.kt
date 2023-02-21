package com.ces.infrastructure.minio

import java.io.File

interface ObjectStorage {
    suspend fun createBucket(bucketName: String)

    suspend fun uploadFile(bucketName: String, localSource: String, storageDestination: String)

    suspend fun downloadFile(bucketName: String, storageSource: String, localDestination: String): File
}