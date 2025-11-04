package com.mobilectl.util

/**
 * Platform-agnostic file operations
 */
expect interface FileOperations {
    fun readFile(path: String): String
    fun writeFile(path: String, content: String)
    fun exists(path: String): Boolean
    fun listDir(path: String): List<String>
    fun createDir(path: String)
    fun deleteFile(path: String)
    fun copyFile(from: String, to: String)
    fun getAbsolutePath(path: String): String
}

expect fun createFileOperations(): FileOperations
