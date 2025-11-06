package com.mobilectl.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import java.io.File

actual interface ProcessExecutor {
    actual suspend fun execute(
        command: String,
        args: List<String>,
        workingDir: String,
        env: Map<String, String>
    ): ProcessResult

    suspend fun executeWithProgress(
        command: String,
        args: List<String>,
        workingDir: String,
        env: Map<String, String> = emptyMap(),
        onProgress: (String) -> Unit
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

    override suspend fun executeWithProgress(
        command: String,
        args: List<String>,
        workingDir: String,
        env: Map<String, String>,
        onProgress: (String) -> Unit
    ): ProcessResult = withContext(Dispatchers.IO) {
        try {
            val processBuilder = ProcessBuilder(command, *args.toTypedArray())
                .directory(File(workingDir))
                .redirectErrorStream(false)

            env.forEach { (key, value) ->
                processBuilder.environment()[key] = value
            }

            val process = processBuilder.start()

            val progressJob = launch {
                val spinner = listOf("⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏")
                var index = 0
                val startTime = System.currentTimeMillis()

                while (isActive && process.isAlive) {
                    val elapsed = (System.currentTimeMillis() - startTime) / 1000
                    onProgress("${spinner[index]} Building... (${elapsed}s)")
                    index = (index + 1) % spinner.size
                    delay(100)
                }
            }

            val stdout = process.inputStream.bufferedReader().readText()
            val stderr = process.errorStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            progressJob.cancel()

            ProcessResult(exitCode, stdout, stderr)
        } catch (e: Exception) {
            ProcessResult(1, "", "Error executing command: ${e.message}")
        }
    }
}
