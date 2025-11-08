package com.mobilectl.builder.android.signing

/**
 * Validated signing configuration
 *
 * Contains resolved paths and credentials
 */
data class SigningConfig(
    val keystorePath: String,
    val keyAlias: String,
    val keyPassword: String,
    val storePassword: String
)

/**
 * Signing validation result
 */
data class SigningValidation(
    val isValid: Boolean,
    val reason: String = "",
    val config: SigningConfig? = null
)
