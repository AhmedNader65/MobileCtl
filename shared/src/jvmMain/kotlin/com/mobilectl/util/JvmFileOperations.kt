package com.mobilectl.util

import java.io.File

actual interface FileOperations {
    actual fun readFile(path: String): String
    actual fun writeFile(path: String, content: String)
    actual fun exists(path: String): Boolean
    actual fun listDir(path: String): List<String>
    actual fun createDir(path: String)
    actual fun deleteFile(path: String)
    actual fun copyFile(from: String, to: String)
    actual fun getAbsolutePath(path: String): String
}

actual fun createFileOperations(): FileOperations = JvmFileOperationsImpl()

class JvmFileOperationsImpl : FileOperations {

    override fun readFile(path: String): String {
        return File(path).readText()
    }

    override fun writeFile(path: String, content: String) {
        File(path).writeText(content)
    }

    override fun exists(path: String): Boolean {
        return File(path).exists()
    }

    override fun listDir(path: String): List<String> {
        return File(path).listFiles()?.map { it.name } ?: emptyList()
    }

    override fun createDir(path: String) {
        File(path).mkdirs()
    }

    override fun deleteFile(path: String) {
        File(path).delete()
    }

    override fun copyFile(from: String, to: String) {
        File(from).copyTo(File(to), overwrite = true)
    }

    override fun getAbsolutePath(path: String): String {
        return File(path).absolutePath
    }
}
