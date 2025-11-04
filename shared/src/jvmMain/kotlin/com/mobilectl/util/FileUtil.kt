package com.mobilectl.util

import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual fun createFileUtil(): FileUtil = JvmFileUtil()

actual interface FileUtil {
    actual suspend fun readFile(path: String): String
    actual suspend fun writeFile(path: String, content: String)
    actual suspend fun exists(path: String): Boolean
    actual suspend fun listDir(path: String): List<String>
    actual suspend fun createDir(path: String): Boolean
    actual suspend fun deleteFile(path: String): Boolean
    actual suspend fun copyFile(from: String, to: String): File
}

class JvmFileUtil : FileUtil {

    override suspend fun readFile(path: String): String = withContext(Dispatchers.IO) {
        File(path).readText()
    }

    override suspend fun writeFile(path: String, content: String) = withContext(Dispatchers.IO) {
        File(path).writeText(content)
    }

    override suspend fun exists(path: String): Boolean = withContext(Dispatchers.IO) {
        File(path).exists()
    }

    override suspend fun listDir(path: String): List<String> = withContext(Dispatchers.IO) {
        File(path).listFiles()?.map { it.name } ?: emptyList()
    }

    override suspend fun createDir(path: String) = withContext(Dispatchers.IO) {
        File(path).mkdirs()
    }

    override suspend fun deleteFile(path: String) = withContext(Dispatchers.IO) {
        File(path).delete()
    }

    override suspend fun copyFile(from: String, to: String) = withContext(Dispatchers.IO) {
        File(from).copyTo(File(to), overwrite = true)
    }
}
