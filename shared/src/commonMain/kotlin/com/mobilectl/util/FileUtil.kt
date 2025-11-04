package com.mobilectl.util

import java.io.File

expect interface FileUtil {
    suspend fun readFile(path: String): String
    suspend fun writeFile(path: String, content: String)
    suspend fun exists(path: String): Boolean
    suspend fun listDir(path: String): List<String>
    suspend fun createDir(path: String): Boolean
    suspend fun deleteFile(path: String): Boolean
    suspend fun copyFile(from: String, to: String): File
}

expect fun createFileUtil(): FileUtil
