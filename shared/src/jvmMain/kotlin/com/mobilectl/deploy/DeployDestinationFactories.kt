package com.mobilectl.deploy

import com.mobilectl.model.deploy.DeployResult
import java.io.File

/**
 * Create default Android deployment destinations
 */
actual fun createDefaultAndroidDestinations(): Map<String, suspend (File, Map<String, String>) -> DeployResult> {
    val firebaseUploader = FirebaseAndroidUploader()
    val localUploader = LocalAndroidUploader()

    return mapOf(
        "firebase" to { file, config ->
            firebaseUploader.upload(file, config)
        },
        "local" to { file, config ->
            localUploader.upload(file, config)
        }
    )
}

/**
 * Create default iOS deployment destinations
 */
actual fun createDefaultIosDestinations(): Map<String, suspend (File, Map<String, String>) -> DeployResult> {
    val testflightUploader = TestFlightUploader()
    val localUploader = LocalIosUploader()

    return mapOf(
        "testflight" to { file, config ->
            testflightUploader.upload(file, config)
        },
        "local" to { file, config ->
            localUploader.upload(file, config)
        }
    )
}
