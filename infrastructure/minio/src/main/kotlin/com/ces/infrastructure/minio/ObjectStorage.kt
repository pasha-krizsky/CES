package com.ces.infrastructure.minio

import java.io.File

interface ObjectStorage {
    suspend fun createBucket(bucketName: String)

    suspend fun uploadFile(bucketName: String, fromPath: String, toPath: String)

    suspend fun downloadFile(bucketName: String, fromPath: String, toPath: String): File
}