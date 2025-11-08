package com.mobilectl.builder.android.signing

import com.mobilectl.util.ProcessExecutor

/**
 * Factory for creating SigningOrchestrator
 */
object SigningOrchestratorFactory {

    fun create(
        processExecutor: ProcessExecutor,
        sdkFinder: AndroidSdkFinder
    ): SigningOrchestrator {
        val apkSigner = JvmApkSigner(processExecutor, sdkFinder)
        val aabSigner = JvmAabSigner(processExecutor)

        return JvmSigningOrchestrator(apkSigner, aabSigner)
    }
}