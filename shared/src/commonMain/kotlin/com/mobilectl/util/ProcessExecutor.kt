package com.mobilectl.util

data class ProcessResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String
) {
    val success: Boolean get() = exitCode == 0
}

interface ProcessExecutor {
    suspend fun execute(
        command: String,
        args: List<String> = emptyList(),
        workingDir: String = ".",
        env: Map<String, String> = emptyMap()
    ): ProcessResult

    suspend fun executeWithProgress(
        command: String,
        args: List<String>,
        workingDir: String,
        env: Map<String, String> = emptyMap(),
        onProgress: (String) -> Unit
    ): ProcessResult
}

expect fun createProcessExecutor(): ProcessExecutor
