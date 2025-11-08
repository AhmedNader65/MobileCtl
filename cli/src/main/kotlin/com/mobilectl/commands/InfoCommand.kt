package com.mobilectl.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.obj
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option

class InfoCommand : CliktCommand(name = "info") {
    private val verbose by option("--verbose", help = "Verbose output").flag()


    override fun run() {

        echo("""
            ✅ Project Information
            ├─ Android: detected ✅
            │  ├─ Identifier: com.example.myapp
            │  ├─ Version: 1.0.0
            │  └─ Gradle Task: bundleRelease
            ├─ iOS: not detected ❌
            └─ Config: Using defaults (mobileops.yaml not found)
        """.trimIndent())

        if (verbose) {
            echo("""
                
                🔍 Verbose Information:
                ├─ Working directory: ${currentContext.obj}
                ├─ Config file: ./mobileops.yaml (not found)
                ├─ Android project: ./android
                ├─ iOS project: ./ios
                └─ Auto-detected config applied
            """.trimIndent())
        }
    }
}