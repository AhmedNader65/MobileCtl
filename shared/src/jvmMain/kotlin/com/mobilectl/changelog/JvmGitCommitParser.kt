package com.mobilectl.changelog

import com.mobilectl.model.changelog.CommitType
import com.mobilectl.model.changelog.GitCommit
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.lib.ObjectId

class JGitCommitParser : GitCommitParser {

    private val repository: Repository? by lazy {
        try {
            FileRepositoryBuilder()
                .findGitDir(File("."))
                .build()
        } catch (e: Exception) {
            null
        }
    }

    override fun parseCommits(fromTag: String?, toTag: String?): List<GitCommit> {
        return try {
            val repo = repository ?: return emptyList()

            RevWalk(repo).use { revWalk ->
                // Get commit range
                val startId = if (fromTag != null) {
                    repo.refDatabase.getRef(fromTag)?.objectId
                } else {
                    null
                }

                val endId = if (toTag != null) {
                    repo.refDatabase.getRef(toTag)?.objectId
                } else {
                    repo.refDatabase.getRef("HEAD")?.objectId
                }

                if (endId == null) return emptyList()

                val endCommit = revWalk.parseCommit(endId)
                revWalk.markStart(endCommit)

                if (startId != null) {
                    val startCommit = revWalk.parseCommit(startId)
                    revWalk.markUninteresting(startCommit)
                }

                // Collect commits
                revWalk.map { commit ->
                    val subject = commit.shortMessage
                    val body = commit.fullMessage.substringAfter("\n\n")

                    GitCommit(
                        hash = commit.name,
                        shortHash = commit.name.take(7),
                        type = extractType(subject),
                        scope = extractScope(subject),
                        message = extractMessage(subject),
                        body = body.takeIf { it.isNotBlank() },
                        author = commit.authorIdent.name,
                        date = Instant.ofEpochSecond(commit.commitTime.toLong())
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate(),
                        breaking = body.contains("BREAKING CHANGE:")
                    )
                }.toList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun parseCommitsSinceHash(sinceHash: String): List<GitCommit> {
        return parseCommits(fromTag = sinceHash, toTag = null)
    }

    override fun getLatestTag(): String? {
        return try {
            val repo = repository ?: return null

            repo.refDatabase.refs.filter { it.name.startsWith("refs/tags/") }
                .sortedByDescending { it.name }
                .firstOrNull()
                ?.name
                ?.removePrefix("refs/tags/")
        } catch (e: Exception) {
            null
        }
    }

    override fun getAllTags(): List<String> {
        return try {
            val repo = repository ?: return emptyList()

            repo.refDatabase.refs.filter { it.name.startsWith("refs/tags/") }
                .map { it.name.removePrefix("refs/tags/") }
                .sortedByDescending { it }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun getTagDate(tag: String): LocalDate? {
        return try {
            val repo = repository ?: return null
            val ref = repo.refDatabase.getRef(tag) ?: return null

            RevWalk(repo).use { revWalk ->
                val commit = revWalk.parseCommit(ref.objectId)
                Instant.ofEpochSecond(commit.commitTime.toLong())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
            }
        } catch (e: Exception) {
            null
        }
    }

    override fun getCompareUrl(fromTag: String, toTag: String): String {
        return try {
            val repo = repository ?: return "#"
            val config = repo.config
            val remoteUrl = config.getString("remote", "origin", "url") ?: return "#"

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

    override fun parseCommitMessage(message: String, commitTypes: List<CommitType>): GitCommit? {
        val pattern = """^(\w+)(?:\(([^)]+)\))?:\s*(.+)$""".toRegex()
        val match = pattern.find(message) ?: return null

        return GitCommit(
            hash = "",
            shortHash = "",
            type = match.groupValues[1],
            scope = match.groupValues[2].takeIf { it.isNotEmpty() },
            message = match.groupValues[3]
        )
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
}
