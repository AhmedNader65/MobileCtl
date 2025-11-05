package com.mobilectl.model.changelog

import java.time.LocalDate

data class GitCommit(
    val hash: String,
    val shortHash: String,
    val type: String,
    val scope: String?,
    val message: String,
    val body: String? = null,
    val author: String? = null,
    val date: LocalDate? = null,
    val breaking: Boolean = false
)
