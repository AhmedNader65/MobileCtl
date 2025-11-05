package com.mobilectl.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.mobilectl.commands.changelog.ChangelogGenerateHandler
import com.mobilectl.commands.changelog.ChangelogRestoreHandler
import com.mobilectl.commands.changelog.ChangelogShowHandler
import com.mobilectl.commands.changelog.ChangelogUpdateHandler
import kotlinx.coroutines.runBlocking

class ChangelogCommand : CliktCommand(name = "changelog") {
    override fun run() = Unit
}

class ChangelogGenerateCommand : CliktCommand(name = "generate") {
    private val fromTag by option("--from-tag", help = "Generate from specific tag")
    private val verbose by option("--verbose", "-v").flag()
    private val dryRun by option("--dry-run").flag()
    private val append by option("--append").flag(default = true)
    private val overwrite by option("--overwrite").flag()
    private val freshStart by option("--fresh", help = "Ignore last state, start from beginning").flag()

    override fun run() {
        runBlocking {
            ChangelogGenerateHandler(
                fromTag = fromTag,
                verbose = verbose,
                dryRun = dryRun,
                append = append && !overwrite,
                useLastState = !freshStart
            ).execute()
        }
    }
}


class ChangelogShowCommand : CliktCommand(name = "show") {
    private val verbose by option("--verbose", "-v").flag()

    override fun run() {
        runBlocking {
            ChangelogShowHandler(verbose = verbose).execute()
        }
    }
}

class ChangelogUpdateCommand : CliktCommand(name = "update") {
    private val verbose by option("--verbose", "-v").flag()

    override fun run() {
        runBlocking {
            ChangelogUpdateHandler(verbose = verbose).execute()
        }
    }
}
class ChangelogRestoreCommand : CliktCommand(name = "restore") {
    private val backupId by argument("backup-id", help = "Backup to restore").optional()
    private val verbose by option("--verbose", "-v").flag()

    override fun run() {
        runBlocking {
            ChangelogRestoreHandler(backupId, verbose).execute()
        }
    }
}

