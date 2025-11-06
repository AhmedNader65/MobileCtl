package com.mobilectl.commands.deploy

import com.mobilectl.model.deploy.DeployResult
import com.mobilectl.util.ArtifactDetector
import java.io.File
import java.io.PrintWriter
import java.nio.charset.StandardCharsets

private val out = PrintWriter(System.out, true, StandardCharsets.UTF_8)

internal fun printDeployResults(results: List<DeployResult>, verbose: Boolean, workingPath: String) {
    val successful = results.filter { it.success }
    val failed = results.filter { !it.success }

    if (successful.isNotEmpty()) {
        successful.forEach { result ->
            printSuccessfulResult(result, verbose)
        }
    }

    if (failed.isNotEmpty()) {
        failed.forEach { result ->
            printFailedResult(result, verbose)
        }
    }

    printOverallStatus(successful, failed)

    if (failed.isNotEmpty()) {
        printRecommendations(failed, workingPath)
    }
}

private fun printSuccessfulResult(result: DeployResult, verbose : Boolean) {
    val platform = result.platform.uppercase()
    val dest = result.destination.uppercase()

    val items = mutableMapOf<String, String>()
    items["Platform"] = "$platform → $dest"
    items["Status"] = result.message

    if (result.buildId != null) {
        items["Release ID"] = result.buildId!!
    }

    if (result.buildUrl != null) {
        items["Console"] = result.buildUrl!!
    }

    if (verbose && result.duration > 0) {
        val seconds = result.duration / 1000.0
        items["Duration"] = "${String.format("%.2f", seconds)}s"
    }

    com.mobilectl.util.PremiumLogger.box("Deployment Successful", items, success = true)
}

private fun printFailedResult(result: DeployResult, verbose: Boolean) {
    val platform = result.platform.uppercase()
    val dest = result.destination.uppercase()

    val items = mutableMapOf<String, String>()
    items["Platform"] = "$platform → $dest"
    items["Error"] = result.message

    if (result.error != null && verbose) {
        items["Details"] = result.error!!.message ?: "Unknown error"
    }

    com.mobilectl.util.PremiumLogger.box("Deployment Failed", items, success = false)
}

private fun printOverallStatus(
    successful: List<DeployResult>,
    failed: List<DeployResult>
) {
    val total = successful.size + failed.size

    if (failed.isEmpty()) {
        com.mobilectl.util.PremiumLogger.simpleSuccess("All $total deployment(s) completed successfully!")

        val totalDuration = successful.sumOf { it.duration }
        if (totalDuration > 0) {
            val seconds = totalDuration / 1000.0
            com.mobilectl.util.PremiumLogger.info("Total time: ${String.format("%.2f", seconds)}s")
        }
        println()
    } else {
        com.mobilectl.util.PremiumLogger.simpleWarning("${successful.size}/$total successful, ${failed.size} failed")

        if (successful.isNotEmpty()) {
            com.mobilectl.util.PremiumLogger.info("Successful: ${successful.map { it.destination }.joinToString(", ")}")
        }
        com.mobilectl.util.PremiumLogger.info("Failed: ${failed.map { it.destination }.joinToString(", ")}")
        println()
    }
}

/**
 * Print actionable recommendations based on failures
 */
private fun printRecommendations(failedResults: List<DeployResult>, workingPath: String) {
    out.println("💡 Troubleshooting Tips:")
    out.println()

    failedResults.forEach { result ->
        val recommendations = getRecommendations(result, workingPath)
        recommendations.forEach { (title, tips) ->
            out.println("📌 $title")
            tips.forEach { tip ->
                out.println("   • $tip")
            }
            out.println()
        }
    }

    out.println("🔧 General Tips:")
    out.println("   • Run with --verbose flag for more details")
    out.println("   • Check 'mobileops.yaml' configuration")
    out.println("   • Verify credentials and permissions")
    out.println("   • Check your internet connection")
}

/**
 * Get recommended solutions based on error message
 */
private fun getRecommendations(
    result: DeployResult,
    workingPath: String
): Map<String, List<String>> {
    val message = result.message.lowercase()
    val recommendations = mutableMapOf<String, List<String>>()
    when {
        // Firebase errors
        message.contains("Service account", true) || message.contains("credential", true) -> {
            recommendations["Firebase Authentication"] = listOf(
                "Download service account from Firebase Console",
                "Go to Project Settings → Service Accounts",
                "Place file at: credentials/firebase-service-account.json",
                "Verify file has valid JSON format"
            )
        }

        message.contains("google-services.json", true) -> {
            recommendations["Google Services Configuration"] = listOf(
                "Download google-services.json from Firebase Console",
                "Place it in: app/ directory",
                "Alternatively, set: deploy.android.firebase.google_services",
                "Run: mobilectl validate to check configuration"
            )
        }

        message.contains("artifact", true) || message.contains("apk", true) -> {
            recommendations["Build Artifact"] = listOf(
                "Build your app: ./gradlew assembleRelease",
                "Check artifact path in mobileops.yaml",
                "Or rebuild: ./gradlew clean assembleRelease",
                "Verify APK exists: ${ArtifactDetector.findAndroidApk(File(workingPath))?.absolutePath ?: "Not found"}"
            )
        }

        message.contains("test group", true) -> {
            recommendations["Firebase Test Groups"] = listOf(
                "Create test groups in Firebase Console",
                "Go to App Distribution → Testers & Groups",
                "Add testers to groups first",
                "Then specify groups in mobileops.yaml: deploy.android.firebase.test_groups"
            )
        }

        message.contains("permission", true) || message.contains("unauthorized", true) -> {
            recommendations["Access Permissions"] = listOf(
                "Verify service account has required roles:",
                "  - Firebase Admin (for Firebase)",
                "  - Play Console Admin (for Play Store)",
                "Check IAM roles in Google Cloud Console",
                "Regenerate service account if needed"
            )
        }

        message.contains("network", true) || message.contains("connection", true) -> {
            recommendations["Network Connectivity"] = listOf(
                "Check internet connection",
                "Verify firewall allows Firebase/Play Console access",
                "Try again in a moment (servers may be busy)",
                "Check service status: https://status.firebase.google.com"
            )
        }

        message.contains("already exists", true) || message.contains("duplicate", true) -> {
            recommendations["Release Already Exists"] = listOf(
                "Increment version code in build.gradle",
                "Or check Firebase Console for existing releases",
                "Deploy to a different test group",
                "Or skip-build and deploy with new version"
            )
        }

        message.contains("invalid", true) -> {
            recommendations["Invalid Configuration"] = listOf(
                "Review error message above carefully",
                "Check mobileops.yaml syntax (YAML format)",
                "Verify all required fields are filled",
                "Run: mobilectl validate to check configuration"
            )
        }

        message.contains("not found", true) -> {
            recommendations["File Not Found"] = listOf(
                "Check file paths in mobileops.yaml",
                "Use absolute or relative-to-project paths",
                "Verify file permissions (readable)",
                "For artifacts: Build your app first"
            )
        }

        message.contains("timeout", true) -> {
            recommendations["Request Timeout"] = listOf(
                "Network connection may be slow",
                "Increase timeout in mobileops.yaml",
                "Try deploying again",
                "Contact Firebase support if issue persists"
            )
        }

        else -> {
            recommendations["General Troubleshooting"] = listOf(
                "Read the error message carefully above",
                "Run with --verbose for more details: mobilectl deploy --verbose",
                "Check mobilectl documentation",
                "Review Firebase/Play Console settings",
                "Create an issue with error details"
            )
        }
    }

    return recommendations
}
