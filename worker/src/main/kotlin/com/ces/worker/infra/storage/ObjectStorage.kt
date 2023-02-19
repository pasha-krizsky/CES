package com.ces.worker.infra.storage

interface ObjectStorage {
    suspend fun createBucket(bucketName: String)

    suspend fun uploadFile(bucketName: String, localSource: String, storageDestination: String)

    suspend fun downloadFile(bucketName: String, storageSource: String, localDestination: String)
}