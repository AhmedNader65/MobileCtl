package com.mobilectl.commands.deploy

import com.mobilectl.model.deploy.DeployResult
import com.mobilectl.util.ArtifactDetector
import java.io.File
import java.io.PrintWriter
import java.nio.charset.StandardCharsets

private val out = PrintWriter(System.out, true, StandardCharsets.UTF_8)

internal fun printDeployResults(results: List<DeployResult>, verbose: Boolean, workingPath: String) {
    out.println()
    out.println("📊 Deployment Summary:")
    out.println()

    // Separate successful and failed results
    val successful = results.filter { it.success }
    val failed = results.filter { !it.success }

    // Print successful deployments
    successful.forEach { result ->
        printSuccessfulResult(result, verbose)
    }

    // Print failed deployments with detailed error info
    failed.forEach { result ->
        printFailedResult(result, verbose)
    }

    out.println()

    // Print overall status
    printOverallStatus(successful, failed)

    // Print recommendations if there are failures
    if (failed.isNotEmpty()) {
        out.println()
        printRecommendations(failed, workingPath)
    }
}

/**
 * Print successful deployment result
 */
private fun printSuccessfulResult(result: DeployResult, verbose : Boolean) {
    val platform = result.platform.uppercase()
    val dest = result.destination.uppercase()

    out.println("✅ $platform → $dest")
    out.println("   ${result.message}")

    if (result.buildUrl != null) {
        out.println("   🔗 ${result.buildUrl}")
    }

    if (result.buildId != null) {
        out.println("   ID: ${result.buildId}")
    }

    if (verbose && result.duration > 0) {
        val seconds = result.duration / 1000.0
        out.println("   ⏱️  ${String.format("%.2f", seconds)}s")
    }

    out.println()
}

/**
 * Print failed deployment result with error details
 */
private fun printFailedResult(result: DeployResult, verbose: Boolean) {
    val platform = result.platform.uppercase()
    val dest = result.destination.uppercase()

    out.println("❌ $platform → $dest")
    out.println("   Error: ${result.message}")

    if (result.error != null) {
        out.println("   Details: ${result.error!!.message}")
        if (verbose) {
            // Print stack trace only in very verbose mode
            result.error!!.stackTrace.take(3).forEach { stackTrace ->
                out.println("     at ${stackTrace.methodName}(${stackTrace.fileName}:${stackTrace.lineNumber})")
            }
        }
    }

    out.println()
}

/**
 * Print overall deployment status
 */
private fun printOverallStatus(
    successful: List<DeployResult>,
    failed: List<DeployResult>
) {
    val total = successful.size + failed.size

    if (failed.isEmpty()) {
        out.println("✅ All $total deployment(s) completed successfully!")

        val totalDuration = successful.sumOf { it.duration }
        if (totalDuration > 0) {
            val seconds = totalDuration / 1000.0
            out.println("⏱️  Total time: ${String.format("%.2f", seconds)}s")
        }
    } else {
        out.println("⚠️  Deployment Status: ${successful.size}/$total successful, ${failed.size} failed")

        if (successful.isNotEmpty()) {
            out.println("   ✅ Successful: ${successful.map { it.destination }.joinToString(", ")}")
        }
        out.println("   ❌ Failed: ${failed.map { it.destination }.joinToString(", ")}")
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
    out.println("   • Check 'mobilectl.yaml' configuration")
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
                "Check artifact path in mobilectl.yaml",
                "Or rebuild: ./gradlew clean assembleRelease",
                "Verify APK exists: ${ArtifactDetector.findAndroidApk(File(workingPath))?.absolutePath ?: "Not found"}"
            )
        }

        message.contains("test group", true) -> {
            recommendations["Firebase Test Groups"] = listOf(
                "Create test groups in Firebase Console",
                "Go to App Distribution → Testers & Groups",
                "Add testers to groups first",
                "Then specify groups in mobilectl.yaml: deploy.android.firebase.test_groups"
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
                "Check mobilectl.yaml syntax (YAML format)",
                "Verify all required fields are filled",
                "Run: mobilectl validate to check configuration"
            )
        }

        message.contains("not found", true) -> {
            recommendations["File Not Found"] = listOf(
                "Check file paths in mobilectl.yaml",
                "Use absolute or relative-to-project paths",
                "Verify file permissions (readable)",
                "For artifacts: Build your app first"
            )
        }

        message.contains("timeout", true) -> {
            recommendations["Request Timeout"] = listOf(
                "Network connection may be slow",
                "Increase timeout in mobilectl.yaml",
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
