package com.mobilectl.desktop.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File

/**
 * Executes processes with real-time line-by-line output streaming
 * Used for build and deployment operations in the desktop GUI
 */
class StreamingProcessExecutor {

    /**
     * Execute a command and stream output line-by-line
     * @param command Command to execute (e.g., "gradlew", "mobilectl")
     * @param args Command arguments
     * @param workingDir Working directory for the process
     * @param env Environment variables
     * @return Flow of output lines (stdout and stderr combined)
     */
    fun executeWithStreaming(
        command: String,
        args: List<String>,
        workingDir: String = System.getProperty("user.dir"),
        env: Map<String, String> = emptyMap()
    ): Flow<ProcessOutput> = flow {
        withContext(Dispatchers.IO) {
            try {
                val processBuilder = ProcessBuilder(command, *args.toTypedArray())
                    .directory(File(workingDir))
                    .redirectErrorStream(false) // Keep stderr separate

                // Add environment variables
                env.forEach { (key, value) ->
                    processBuilder.environment()[key] = value
                }

                val process = processBuilder.start()

                // Read stdout and stderr in parallel
                val stdoutReader = process.inputStream.bufferedReader()
                val stderrReader = process.errorStream.bufferedReader()

                // Emit initial status
                emit(ProcessOutput.Started(command))

                // Read output line by line from both streams
                var stdoutDone = false
                var stderrDone = false

                while (!stdoutDone || !stderrDone) {
                    // Check stdout
                    if (!stdoutDone) {
                        val line = stdoutReader.readLineOrNull()
                        if (line != null) {
                            emit(ProcessOutput.StdOut(line))
                        } else {
                            stdoutDone = true
                        }
                    }

                    // Check stderr
                    if (!stderrDone) {
                        val line = stderrReader.readLineOrNull()
                        if (line != null) {
                            emit(ProcessOutput.StdErr(line))
                        } else {
                            stderrDone = true
                        }
                    }
                }

                // Wait for process to complete
                val exitCode = process.waitFor()
                emit(ProcessOutput.Completed(exitCode))

            } catch (e: Exception) {
                emit(ProcessOutput.Error(e.message ?: "Unknown error", e))
            }
        }
    }

    /**
     * Execute gradle build command with streaming
     */
    fun executeBuild(
        platform: String,
        flavor: String,
        buildType: String = "release",
        workingDir: String = System.getProperty("user.dir")
    ): Flow<ProcessOutput> {
        val gradleCmd = if (System.getProperty("os.name").startsWith("Windows")) {
            "gradlew.bat"
        } else {
            "./gradlew"
        }

        val task = when (platform.lowercase()) {
            "android" -> "assemble${flavor.capitalize()}${buildType.capitalize()}"
            "ios" -> "linkReleaseFrameworkIos" // KMP iOS
            else -> throw IllegalArgumentException("Unknown platform: $platform")
        }

        return executeWithStreaming(
            command = gradleCmd,
            args = listOf(task, "--stacktrace"),
            workingDir = workingDir
        )
    }

    /**
     * Execute mobilectl deploy command with streaming
     */
    fun executeDeploy(
        platform: String,
        flavor: String,
        track: String,
        workingDir: String = System.getProperty("user.dir")
    ): Flow<ProcessOutput> {
        // Check if mobilectl is installed, otherwise use jar
        val command = "mobilectl" // TODO: Detect if available, otherwise use java -jar

        return executeWithStreaming(
            command = command,
            args = listOf(
                "deploy",
                "--platform", platform,
                "--flavor", flavor,
                "--track", track,
                "--verbose"
            ),
            workingDir = workingDir
        )
    }

    private fun BufferedReader.readLineOrNull(): String? {
        return try {
            if (ready()) readLine() else null
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Represents different types of process output
 */
sealed class ProcessOutput {
    data class Started(val command: String) : ProcessOutput()
    data class StdOut(val line: String) : ProcessOutput()
    data class StdErr(val line: String) : ProcessOutput()
    data class Completed(val exitCode: Int) : ProcessOutput()
    data class Error(val message: String, val exception: Throwable? = null) : ProcessOutput()
}

/**
 * Helper to capitalize first letter
 */
private fun String.capitalize(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}
