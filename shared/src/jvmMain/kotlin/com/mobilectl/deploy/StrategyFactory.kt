package com.mobilectl.deploy

/**
 * Factory for creating upload strategy instances
 * Implementation is platform-specific (JVM, Android, iOS)
 */
actual fun createDefaultAndroidStrategies(): Map<String, UploadStrategy> = mapOf(
    "firebase" to FirebaseAndroidUploader(),
    "local" to LocalAndroidUploader()
)

actual fun createDefaultIosStrategies(): Map<String, UploadStrategy> = mapOf(
    "testflight" to TestFlightUploader(),
    "local" to LocalIosUploader()
)
