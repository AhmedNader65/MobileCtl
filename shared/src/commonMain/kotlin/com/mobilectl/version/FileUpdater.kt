package com.mobilectl.version

expect fun createFileUpdater(): FileUpdater
interface FileUpdater {
    fun updateVersionInFiles(
        oldVersion: String,
        newVersion: String,
        filesToUpdate: List<String>
    ): List<String>
    fun updateConfig(configPath: String, newVersion: String): Boolean
}