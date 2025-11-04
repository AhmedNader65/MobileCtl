package com.mobilectl.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

actual interface ProcessExecutor {
    actual suspend fun execute(
        command: String,
        args: List<String>,
        workingDir: String,
        env: Map<String, String>
    ): ProcessResult
}

actual fun createProcessExecutor(): ProcessExecutor = JvmProcessExecutorImpl()

class JvmProcessExecutorImpl : ProcessExecutor {
    override suspend fun execute(
        command: String,
        args: List<String>,
        workingDir: String,
        env: Map<String, String>
    ): ProcessResult = withContext(Dispatchers.IO) {
        try {
            val processBuilder = ProcessBuilder(command, *args.toTypedArray())
                .directory(File(workingDir))
                .redirectErrorStream(false)

            // Add environment variables
            env.forEach { (key, value) ->
                processBuilder.environment()[key] = value
            }

            val process = processBuilder.start()

            val stdout = process.inputStream.bufferedReader().readText()
            val stderr = process.errorStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            ProcessResult(exitCode, stdout, stderr)
        } catch (e: Exception) {
            ProcessResult(1, "", "Error executing command: ${e.message}")
        }
    }
}
