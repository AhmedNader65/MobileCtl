package com.mobilectl.deploy.firebase

import com.mobilectl.util.PremiumLogger
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.SerializationException
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.*

@Serializable
data class ServiceAccountKey(
    val type: String? = null,
    val project_id: String? = null,
    val private_key_id: String? = null,
    val private_key: String,
    val client_email: String,
    val client_id: String? = null,
    val auth_uri: String? = null,
    val token_uri: String,
    val auth_provider_x509_cert_url: String? = null,
    val client_x509_cert_url: String? = null
)

@Serializable
data class AccessTokenResponse(
    val access_token: String,
    val expires_in: Int,
    val token_type: String
)

/**
 * Service Account Authentication
 * Generates JWT and exchanges for access token
 */
object ServiceAccountAuth {
    object Scopes {
        const val CLOUD_PLATFORM = "https://www.googleapis.com/auth/cloud-platform"
        const val ANDROID_PUBLISHER = "https://www.googleapis.com/auth/androidpublisher"
    }

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    private const val JWT_EXPIRY_SECONDS = 3600L

    suspend fun getAccessTokenFromServiceAccount(
        serviceAccountFile: File,
        scope: String = Scopes.CLOUD_PLATFORM
    ): String {
        try {
            if (!serviceAccountFile.exists()) {
                throw Exception("Service account file not found: ${serviceAccountFile.absolutePath}")
            }

            val json = serviceAccountFile.readText()

            if (json.isBlank()) {
                throw Exception("Service account file is empty")
            }

            val account = try {
                jsonParser.decodeFromString<ServiceAccountKey>(json)
            } catch (e: SerializationException) {
                throw Exception(
                    "Invalid service account JSON format.\n" +
                            "Required fields: private_key, client_email, token_uri\n" +
                            "Error: ${e.message}"
                )
            }

            validateServiceAccountKey(account)

            PremiumLogger.detail("Service Account", account.client_email)

            val jwt = createJwt(account, scope)
            val token = exchangeJwtForToken(jwt, account.token_uri)

            val expiryText = com.mobilectl.util.TimeFormatter.formatExpiry(token.expires_in)
            PremiumLogger.detail(
                "Access Token",
                "Valid for $expiryText",
                dim = true
            )

            return token.access_token

        } catch (e: Exception) {
            throw Exception("Failed to get access token: ${e.message}", e)
        }
    }

    /**
     * Validate that service account has required fields
     */
    private fun validateServiceAccountKey(account: ServiceAccountKey) {
        val errors = mutableListOf<String>()

        if (account.private_key.isBlank()) {
            errors.add("private_key is required")
        }

        if (account.client_email.isBlank()) {
            errors.add("client_email is required")
        }

        if (account.token_uri.isBlank()) {
            errors.add("token_uri is required")
        }

        if (account.private_key.contains("-----BEGIN")) {
            // Key looks like PEM format, good
        } else if (!account.private_key.contains("\\n")) {
            errors.add("private_key appears to be in wrong format (should be PEM with \\n)")
        }

        if (errors.isNotEmpty()) {
            throw Exception(
                "Invalid service account configuration:\n" +
                        errors.joinToString("\n") { "  • $it" }
            )
        }
    }

    private fun createJwt(account: ServiceAccountKey, scope: String): String {
        val now = System.currentTimeMillis() / 1000
        val expiry = now + JWT_EXPIRY_SECONDS

        val headerJson = """{"alg":"RS256","typ":"JWT"}"""
        val payloadJson = """{
            "iss":"${account.client_email}",
            "scope":"$scope",
            "aud":"${account.token_uri}",
            "exp":$expiry,
            "iat":$now
        }""".replace(Regex("\\s+"), "")

        val headerEncoded = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(headerJson.toByteArray())
        val payloadEncoded = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(payloadJson.toByteArray())

        val signatureInput = "$headerEncoded.$payloadEncoded"
        val signature = signWithPrivateKey(signatureInput, account.private_key)
        val signatureEncoded = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(signature)

        return "$signatureInput.$signatureEncoded"
    }

    private fun signWithPrivateKey(input: String, privateKeyPem: String): ByteArray {
        try {
            val privateKeyString = privateKeyPem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("\n", "")
                .replace("\\n", "")  // Handle escaped newlines
                .trim()

            if (privateKeyString.isEmpty()) {
                throw Exception("Private key is empty after cleanup")
            }

            val decodedKey = try {
                Base64.getDecoder().decode(privateKeyString)
            } catch (e: IllegalArgumentException) {
                throw Exception("Invalid Base64 in private key: ${e.message}")
            }

            val keyFactory = java.security.KeyFactory.getInstance("RSA")
            val keySpec = java.security.spec.PKCS8EncodedKeySpec(decodedKey)
            val privateKey = keyFactory.generatePrivate(keySpec)

            val signature = java.security.Signature.getInstance("SHA256withRSA")
            signature.initSign(privateKey)
            signature.update(input.toByteArray())

            return signature.sign()

        } catch (e: Exception) {
            throw Exception("Failed to sign JWT: ${e.message}", e)
        }
    }

    private suspend fun exchangeJwtForToken(jwt: String, tokenUri: String): AccessTokenResponse {
        return try {
            val client = OkHttpClient()

            val requestBody =
                "grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer&assertion=$jwt"
                    .toByteArray()

            val request = Request.Builder()
                .url(tokenUri)
                .post(okhttp3.RequestBody.create(null, requestBody))
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    throw Exception("Token exchange failed: HTTP ${response.code}\n$body")
                }

                try {
                    jsonParser.decodeFromString<AccessTokenResponse>(body)
                } catch (e: SerializationException) {
                    throw Exception("Failed to parse token response: ${e.message}\nResponse: $body")
                }
            }

        } catch (e: Exception) {
            throw Exception("JWT token exchange failed: ${e.message}", e)
        }
    }
}
