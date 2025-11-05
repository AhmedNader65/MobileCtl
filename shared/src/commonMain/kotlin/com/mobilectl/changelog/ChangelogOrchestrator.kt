package com.mobilectl.changelog

import com.mobilectl.model.changelog.ChangelogConfig
import com.mobilectl.model.changelog.CommitType
import com.mobilectl.model.changelog.GitCommit

data class GenerateResult(
    val success: Boolean,
    val changelogPath: String? = null,
    val version: String? = null,
    val commitCount: Int = 0,
    val content: String? = null,
    val error: String? = null
)

interface ChangelogWriter {
    fun write(content: String, filePath: String): Boolean
    fun read(filePath: String): String?
}

class ChangelogOrchestrator(
    private val parser: GitCommitParser,
    private val writer: ChangelogWriter,
    private val stateManager: ChangelogStateManager,
    private val config: ChangelogConfig,
) {

    fun generate(
        fromTag: String? = null,
        toTag: String? = null,
        dryRun: Boolean = false,
        append: Boolean = true,
        useLastState: Boolean = true
    ): GenerateResult {
        return try {
            var actualFromTag = fromTag
            var actualToTag = toTag

            if (actualToTag == null) {
                actualToTag = parser.getLatestTag()
            }

            var isFirstRun = false
            if (useLastState && fromTag == null) {
                val state = stateManager.getState()
                if (state.lastGeneratedCommit != null) {
                    actualFromTag = state.lastGeneratedCommit
                } else {
                    isFirstRun = true
                    actualFromTag = null
                }
            }

            val commits = if (actualFromTag == null) {
                parser.parseCommits(fromTag = null, toTag = actualToTag)
            } else {
                parser.parseCommits(fromTag = actualFromTag, toTag = actualToTag)
            }

            if (commits.isEmpty()) {
                return GenerateResult(
                    success = false,
                    error = "No commits found"
                )
            }

            val commitsByType = groupCommitsByType(commits)
            val breakingCommits = commits.filter { it.breaking }
            val contributors = extractContributors(commits)
            val stats = generateStats(commits)

            val newContent = when (config.format) {
                "markdown" -> generateMarkdown(
                    commitsByType,
                    breakingCommits,
                    contributors,
                    stats,
                    actualToTag
                )

                "html" -> generateHtml(
                    commitsByType,
                    breakingCommits,
                    contributors,
                    stats,
                    actualToTag
                )

                "json" -> generateJson(
                    commitsByType,
                    breakingCommits,
                    contributors,
                    stats,
                    actualToTag
                )

                else -> generateMarkdown(
                    commitsByType,
                    breakingCommits,
                    contributors,
                    stats,
                    actualToTag
                )
            }

            if (dryRun) {
                return GenerateResult(
                    success = true,
                    content = newContent,
                    commitCount = commits.size
                )
            }

            val existingContent = writer.read(config.outputFile)
            val finalContent =
                if (append && existingContent != null && existingContent.isNotEmpty()) {
                    appendChangelog(newContent, existingContent, config.format)
                } else {
                    newContent
                }

            val written = writer.write(finalContent, config.outputFile)

            if (written) {
                val latestCommit = commits.firstOrNull()?.hash
                val newState = ChangelogState(
                    lastGeneratedCommit = latestCommit,
                    lastGeneratedDate = java.time.LocalDateTime.now().toString(),
                    lastGeneratedVersion = actualToTag ?: "HEAD",
                    lastGeneratedRange = if (isFirstRun) {
                        "all commits..${actualToTag ?: "HEAD"}"
                    } else {
                        "${actualFromTag?.take(7) ?: "HEAD"}..${actualToTag ?: "HEAD"}"
                    }
                )
                stateManager.saveState(newState)
            }

            GenerateResult(
                success = written,
                changelogPath = config.outputFile,
                version = actualToTag,
                commitCount = commits.size,
                content = newContent
            )
        } catch (e: Exception) {
            GenerateResult(
                success = false,
                error = e.message
            )
        }
    }

    private fun extractContributors(commits: List<GitCommit>): Map<String, Int> {
        val contributorCounts = commits
            .groupingBy { it.author ?: "Unknown" }
            .eachCount()
        return contributorCounts.toList()
            .sortedByDescending { it.second }
            .toMap()
    }

    private data class Stats(
        val totalCommits: Int,
        val contributors: Map<String, Int>,
        val commitsByType: Map<String, Int>,
        val breakingChanges: Int
    )

    private fun generateStats(commits: List<GitCommit>): Stats {
        return Stats(
            totalCommits = commits.size,
            contributors = extractContributors(commits),
            commitsByType = commits.groupingBy { it.type }.eachCount(),
            breakingChanges = commits.count { it.breaking }
        )
    }

    private fun groupCommitsByType(commits: List<GitCommit>): Map<CommitType, List<GitCommit>> {
        val commitTypeMap = config.commitTypes.associateBy { it.type }

        return commits
            .groupBy { it.type }
            .mapKeys { (type, _) ->
                commitTypeMap[type] ?: CommitType(type, type.capitalize(), "")
            }
    }

    private fun generateMarkdown(
        commitsByType: Map<CommitType, List<GitCommit>>,
        breakingCommits: List<GitCommit>,
        contributors: Map<String, Int>,
        stats: Stats,
        version: String? = null
    ): String {
        val sb = StringBuilder()

        val versionStr = version?.removePrefix("v") ?: "Unreleased"
        val dateStr = getVersionDate(version)

        sb.append("# Changelog\n\n")
        sb.append("## [$versionStr]${if (dateStr != null) " - $dateStr" else ""}\n\n")

        // Release notes from config
        config.releases[versionStr]?.let { notes ->
            if (notes.highlights != null) {
                sb.append("### 📢 Highlights\n\n")
                sb.append(notes.highlights).append("\n\n")
            }
        }

        // Breaking changes
        if (config.includeBreakingChanges && breakingCommits.isNotEmpty()) {
            sb.append("### 🚨 BREAKING CHANGES\n\n")
            breakingCommits.forEach { commit ->
                val commitUrl = buildCommitUrl(commit.shortHash)
                sb.append("- ${commit.message} ([${commit.shortHash}]($commitUrl))\n")
            }
            sb.append("\n")
        }

        // Commits by type
        commitsByType.forEach { (type, commits) ->
            val nonBreaking = commits.filterNot { it.breaking }
            if (nonBreaking.isNotEmpty()) {
                sb.append("### ${type.emoji} ${type.title}\n\n")
                nonBreaking.forEach { commit ->
                    val scope = commit.scope?.let { "(**$it**)" } ?: ""
                    val commitUrl = buildCommitUrl(commit.shortHash)
                    sb.append("- $scope ${commit.message} ([${commit.shortHash}]($commitUrl))\n")
                }
                sb.append("\n")
            }
        }

        // Compare link
        if (config.includeCompareLinks && version != null) {
            val latestTag = parser.getLatestTag()
            if (latestTag != null && latestTag != version) {
                val compareUrl = parser.getCompareUrl(version, latestTag)
                sb.append("[View all changes](${compareUrl})\n\n")
            }
        }

        // Contributors
        if (config.includeContributors && contributors.isNotEmpty()) {
            sb.append("### 👥 Contributors\n\n")
            contributors.entries.take(10).forEach { (author, count) ->
                sb.append("- $author ($count commits)\n")
            }
            sb.append("\n")
        }

        // Stats
        if (config.includeStats) {
            sb.append("### 📊 Stats\n\n")
            sb.append("- Total commits: ${stats.totalCommits}\n")
            sb.append("- Contributors: ${stats.contributors.size}\n")
            if (stats.breakingChanges > 0) {
                sb.append("- Breaking changes: ${stats.breakingChanges}\n")
            }
            sb.append("\n")
        }

        return sb.toString()
    }

    private fun generateHtml(
        commitsByType: Map<CommitType, List<GitCommit>>,
        breakingCommits: List<GitCommit>,
        contributors: Map<String, Int>,
        stats: Stats,
        version: String? = null
    ): String {
        val markdown =
            generateMarkdown(commitsByType, breakingCommits, contributors, stats, version)
        return markdownToHtml(markdown)
    }

    private fun generateJson(
        commitsByType: Map<CommitType, List<GitCommit>>,
        breakingCommits: List<GitCommit>,
        contributors: Map<String, Int>,
        stats: Stats,
        version: String? = null
    ): String {
        val versionStr = version?.removePrefix("v") ?: "unreleased"
        val dateStr = getVersionDate(version) ?: ""

        // Build contributors JSON manually
        val contributorsJson = contributors.entries
            .joinToString(", ") { (author, count) ->
                "\"${escapeJson(author)}\": $count"
            }

        val breakingJson = breakingCommits
            .joinToString(", ") { commit ->
                """{"message": "${escapeJson(commit.message)}", "hash": "${commit.shortHash}"} """
            }

        val featuresJson = commitsByType.values.flatten()
            .filter { it.type == "feat" && !it.breaking }
            .take(20)
            .joinToString(", ") { commit ->
                """{"message": "${escapeJson(commit.message)}", "hash": "${commit.shortHash}"}"""
            }

        val fixesJson = commitsByType.values.flatten()
            .filter { it.type == "fix" && !it.breaking }
            .take(20)
            .joinToString(", ") { commit ->
                """{"message": "${escapeJson(commit.message)}", "hash": "${commit.shortHash}"}"""
            }

        return """{
                      "version": "$versionStr",
                      "date": "$dateStr",
                      "stats": {
                        "totalCommits": ${stats.totalCommits},
                        "contributors": ${stats.contributors.size},
                        "breakingChanges": ${stats.breakingChanges}
                      },
                      "sections": {
                        "breaking": [$breakingJson],
                        "features": [$featuresJson],
                        "fixes": [$fixesJson]
                      },
                      "contributors": {
                        $contributorsJson
                      }
                    }"""
    }

    private fun escapeJson(text: String): String {
        return text.replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
    }

    private fun getVersionDate(version: String?): String? {
        if (version == null) return null
        return try {
            parser.getTagDate(version)?.toString()
        } catch (e: Exception) {
            null
        }
    }

    private fun markdownToHtml(markdown: String): String {
        // For now, return markdown in pre tag
        // TODO: Integrate flexmark library for proper conversion
        return """
        <!DOCTYPE html>
        <html>
        <head><title>Changelog</title></head>
        <body>
        <pre style="font-family: monospace; white-space: pre-wrap;">
        $markdown
        </pre>
        </body>
        </html>
    """.trimIndent()
    }

    private fun appendChangelog(
        newContent: String,
        existingContent: String,
        format: String
    ): String {
        return when (format) {
            "markdown" -> appendMarkdown(newContent, existingContent)
            "html" -> appendHtml(newContent, existingContent)
            "json" -> appendJson(newContent, existingContent)
            else -> appendMarkdown(newContent, existingContent)
        }
    }

    private fun appendMarkdown(newContent: String, existingContent: String): String {
        // Extract just the version section from new content
        val versionLines = newContent.split("\n")
            .dropWhile { !it.startsWith("##") }  // Skip header until first version

        // Extract existing content after first header
        val existingLines = existingContent.split("\n")
            .dropWhile { !it.startsWith("##") || it == "# Changelog" }

        return """# Changelog

${versionLines.joinToString("\n")}

${existingLines.joinToString("\n").trim()}
""".trimIndent()
    }

    private fun appendHtml(newContent: String, existingContent: String): String {
        // Extract body content from new HTML
        val newBody = extractHtmlBody(newContent)
        val existingBody = extractHtmlBody(existingContent)

        return """<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Changelog</title>
    <style>
        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; margin: 2rem; }
        h1 { color: #222; border-bottom: 3px solid #007bff; }
        h2 { color: #555; margin-top: 2rem; }
        h3 { color: #777; }
        ul { list-style: none; margin-left: 1rem; }
        li { margin: 0.5rem 0; }
        a { color: #007bff; }
    </style>
</head>
<body>
    <h1>Changelog</h1>
    $newBody
    $existingBody
</body>
</html>"""
    }

    private fun appendJson(newContent: String, existingContent: String): String {
        // Parse both JSONs and merge
        return newContent // TODO - implement proper JSON merging
    }

    private fun extractHtmlBody(html: String): String {
        val bodyStart = html.indexOf("<h1>")
        val bodyEnd = html.lastIndexOf("</body>")
        return if (bodyStart >= 0 && bodyEnd > bodyStart) {
            html.substring(bodyStart, bodyEnd)
        } else {
            html
        }
    }
}

private fun buildCommitUrl(hash: String): String {
    // Try to detect git remote and build URL
    return try {
        val remoteUrl = ProcessBuilder("git", "config", "--get", "remote.origin.url")
            .redirectErrorStream(true)
            .start()
            .inputStream.bufferedReader().readText().trim()

        // Convert SSH to HTTPS if needed
        val httpUrl = if (remoteUrl.startsWith("git@")) {
            // git@github.com:user/repo.git → https://github.com/user/repo
            remoteUrl
                .replace(Regex("git@([^:]+):"), "https://$1/")
                .replace(".git$", "")
        } else {
            remoteUrl.replace(".git$", "")
        }

        "$httpUrl/commit/$hash"
    } catch (e: Exception) {
        "#"
    }
}

expect fun createChangelogWriter(): ChangelogWriter
