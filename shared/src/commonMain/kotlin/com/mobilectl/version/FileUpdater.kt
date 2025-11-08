package com.mobilectl.version

expect fun createFileUpdater(): FileUpdater
interface FileUpdater {
    fun updateVersionInFiles(
        oldVersion: String,
        newVersion: String,
        filesToUpdate: List<String>,
        incrementVersionCode: Boolean = true
    ): List<String>
    fun updateConfig(configPath: String, newVersion: String): Boolean
}