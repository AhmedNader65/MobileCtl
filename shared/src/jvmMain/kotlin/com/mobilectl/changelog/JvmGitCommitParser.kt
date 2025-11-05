package com.mobilectl.changelog

import com.mobilectl.model.changelog.CommitType
import com.mobilectl.model.changelog.GitCommit
import com.mobilectl.util.createLogger
import java.time.LocalDate


class JvmGitCommitParser : GitCommitParser {
    private val logger = createLogger("GitCommitParser")

    override fun parseCommitsSinceHash(sinceHash: String): List<GitCommit> {
        return parseCommits(fromTag = "$sinceHash^", toTag = null)
    }

    override fun getLatestTag(): String? {
        return try {
            ProcessBuilder("git", "describe", "--tags", "--abbrev=0")
                .redirectErrorStream(true)
                .start()
                .inputStream.bufferedReader().readText().trim()
                .takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }

    override fun parseCommits(fromTag: String?, toTag: String?): List<GitCommit> {
        return try {
            val range = when {
                fromTag != null && toTag != null -> "$fromTag..$toTag"
                fromTag != null -> "$fromTag..HEAD"
                else -> "HEAD"
            }

            val format = "%h%x00%H%x00%s%x00%an%x00%aI%x1e"  // %x1e = record separator

            val process = ProcessBuilder(
                "git", "log",
                "--format=$format",
                range
            )
                .redirectErrorStream(true)
                .start()

            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()

            output.split("\u001e")  // Split by record separator
                .filter { it.isNotBlank() }
                .mapNotNull { record ->
                    val fields = record.split("\u0000")  // Split by null separator
                    if (fields.size >= 5) {
                        val shortHash = fields[0].trim()
                        val hash = fields[1].trim()
                        val subject = fields[2].trim()
                        val author = fields[3].trim()
                        val dateStr = fields[4].trim()

                        if (subject.isNotEmpty()) {
                            val date = try {
                                LocalDate.parse(dateStr.take(10))
                            } catch (e: Exception) {
                                null
                            }

                            GitCommit(
                                hash = hash,
                                shortHash = shortHash,
                                type = extractType(subject),
                                scope = extractScope(subject),
                                message = extractMessage(subject),
                                body = null,
                                author = author,
                                date = date,
                                breaking = subject.contains("BREAKING")
                            )
                        } else null
                    } else null
                }
        } catch (e: Exception) {
            logger.error("Failed to parse commits: ${e.message}")
            emptyList()
        }
    }

    override fun parseCommitMessage(message: String, commitTypes: List<CommitType>): GitCommit? {
        val pattern = """^(\w+)(?:\(([^)]+)\))?:\s*(.+)$""".toRegex()
        val match = pattern.find(message) ?: return null

        val type = match.groupValues[1]
        val scope = match.groupValues[2].takeIf { it.isNotEmpty() }
        val msg = match.groupValues[3]

        return GitCommit(
            hash = "",
            shortHash = "",
            type = type,
            scope = scope,
            message = msg
        )
    }

    override fun getTagDate(tag: String): LocalDate? {
        return try {
            // git log -1 --format=%aI <tag>
            val process = ProcessBuilder(
                "git", "log", "-1", "--format=%aI", tag
            )
                .redirectErrorStream(true)
                .start()

            val dateStr = process.inputStream.bufferedReader().readText().trim()
            process.waitFor()

            LocalDate.parse(dateStr.take(10))
        } catch (e: Exception) {
            null
        }
    }

    private fun extractType(subject: String): String {
        val pattern = """^(\w+)(?:\([^)]+\))?:""".toRegex()
        return pattern.find(subject)?.groupValues?.get(1) ?: "chore"
    }

    private fun extractScope(subject: String): String? {
        val pattern = """^\w+\(([^)]+)\)""".toRegex()
        return pattern.find(subject)?.groupValues?.get(1)
    }

    private fun extractMessage(subject: String): String {
        val pattern = """^[\w()]*:\s*(.+)$""".toRegex()
        return pattern.find(subject)?.groupValues?.get(1) ?: subject
    }
    override fun getAllTags(): List<String> {
        return try {
            ProcessBuilder("git", "tag", "-l", "--sort=-version:refname")
                .redirectErrorStream(true)
                .start()
                .inputStream.bufferedReader().readText()
                .split("\n")
                .filter { it.isNotBlank() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun getCompareUrl(fromTag: String, toTag: String): String {
        return try {
            val remoteUrl = ProcessBuilder("git", "config", "--get", "remote.origin.url")
                .redirectErrorStream(true)
                .start()
                .inputStream.bufferedReader().readText().trim()

            val httpUrl = if (remoteUrl.startsWith("git@")) {
                remoteUrl
                    .replace(Regex("git@([^:]+):"), "https://$1/")
                    .replace(".git$", "")
            } else {
                remoteUrl.replace(".git$", "")
            }

            "$httpUrl/compare/$fromTag...$toTag"
        } catch (e: Exception) {
            "#"
        }
    }
}