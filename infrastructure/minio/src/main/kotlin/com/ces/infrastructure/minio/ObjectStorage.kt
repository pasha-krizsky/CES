package com.ces.infrastructure.minio

import java.io.File

interface ObjectStorage {
    suspend fun createBucket(bucketName: String)

    suspend fun upload(bucketName: String, fromPath: String, toPath: String)

    suspend fun get(bucketName: String, fromPath: String, toPath: String): File

    suspend fun find(bucketName: String, fromPath: String, toPath: String): File?
}