package com.mobilectl.changelog

import com.mobilectl.model.changelog.CommitType
import com.mobilectl.model.changelog.GitCommit
import java.time.LocalDate

interface GitCommitParser {
    fun parseCommits(fromTag: String? = null, toTag: String? = null): List<GitCommit>
    fun parseCommitsSinceHash(sinceHash: String): List<GitCommit>
    fun getLatestTag(): String?
    fun getAllTags(): List<String>
    fun getTagDate(tag: String): LocalDate?
    fun parseCommitMessage(message: String, commitTypes: List<CommitType>): GitCommit?
    fun getCompareUrl(fromTag: String, toTag: String): String
}