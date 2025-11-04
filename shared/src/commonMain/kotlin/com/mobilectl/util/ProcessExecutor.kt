package com.mobilectl.util

data class ProcessResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String
) {
    val success: Boolean get() = exitCode == 0
}

expect interface ProcessExecutor {
    suspend fun execute(
        command: String,
        args: List<String> = emptyList(),
        workingDir: String = ".",
        env: Map<String, String> = emptyMap()
    ): ProcessResult
}

expect fun createProcessExecutor(): ProcessExecutor
