package com.ces.worker.storage

import kotlinx.coroutines.runBlocking


const val MIN_IO_ENDPOINT = "http://127.0.0.1:9000"

const val ACCESS_KEY = "minioadmin"
const val SECRET_KEY = "minioadmin"

const val BUCKET_NAME = "test-bucket"

const val OBJECT_PATH = "test/commands.txt"
const val FILE_TO_UPLOAD = "E:\\projects\\CES\\worker\\commands.txt"
const val FILE_TO_UPLOAD_2 = "E:\\projects\\CES\\worker\\commands-2.txt"
const val DOWNLOADED_FILE = "E:\\projects\\CES\\worker\\commands-output.txt"

// Docker:
// run -p 9000:9000 -p 9001:9001 quay.io/minio/minio server /data --console-address ":9001"
fun main(): Unit = runBlocking {
    val minioStorage = MinioStorage()
    minioStorage.createBucket(BUCKET_NAME)
    minioStorage.uploadFile(BUCKET_NAME, FILE_TO_UPLOAD, OBJECT_PATH)
    minioStorage.uploadFile(BUCKET_NAME, FILE_TO_UPLOAD_2, OBJECT_PATH)
    minioStorage.downloadFile(BUCKET_NAME, OBJECT_PATH, DOWNLOADED_FILE)
}
