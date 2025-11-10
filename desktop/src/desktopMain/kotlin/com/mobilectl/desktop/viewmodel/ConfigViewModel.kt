package com.mobilectl.desktop.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilectl.config.Config
import com.mobilectl.config.ConfigLoader
import com.mobilectl.detector.ProjectDetector
import com.mobilectl.util.FileUtil
import kotlinx.coroutines.launch

data class FirebaseConfigState(
    val enabled: Boolean = true,
    val serviceAccountPath: String = "credentials/firebase-service-account.json",
    val googleServicesPath: String = "",
    val releaseNotes: String = "Automated upload",
    val testGroups: String = "qa-team"
)

data class PlayConsoleConfigState(
    val enabled: Boolean = false,
    val serviceAccountPath: String = "credentials/play-console-service-account.json",
    val packageName: String = "",
    val track: String = "internal",
    val status: String = "draft"
)

data class SigningConfigState(
    val keystorePath: String = "",
    val storePassword: String = "",
    val keyAlias: String = "",
    val keyPassword: String = ""
)

data class ConfigEditorState(
    val firebase: FirebaseConfigState = FirebaseConfigState(),
    val playConsole: PlayConsoleConfigState = PlayConsoleConfigState(),
    val signing: SigningConfigState = SigningConfigState(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val validationErrors: Map<String, String> = emptyMap()
)

class ConfigViewModel : ViewModel() {
    var state by mutableStateOf(ConfigEditorState())
        private set

    private val configLoader = ConfigLoader(
        fileUtil = FileUtil(),
        projectDetector = ProjectDetector()
    )

    init {
        loadConfig()
    }

    private fun loadConfig() {
        state = state.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                val configResult = configLoader.loadConfig(null)
                configResult.onSuccess { config ->
                    // Map config to state
                    val firebaseConfig = config.deploy?.android?.firebase
                    val playConsoleConfig = config.deploy?.android?.playConsole

                    state = state.copy(
                        firebase = FirebaseConfigState(
                            enabled = firebaseConfig?.enabled ?: true,
                            serviceAccountPath = firebaseConfig?.serviceAccount ?: "",
                            googleServicesPath = firebaseConfig?.googleServices ?: "",
                            releaseNotes = firebaseConfig?.releaseNotes ?: "",
                            testGroups = firebaseConfig?.testGroups?.joinToString(", ") ?: ""
                        ),
                        playConsole = PlayConsoleConfigState(
                            enabled = playConsoleConfig?.enabled ?: false,
                            serviceAccountPath = playConsoleConfig?.serviceAccount ?: "",
                            packageName = playConsoleConfig?.packageName ?: "",
                            track = playConsoleConfig?.track ?: "internal",
                            status = playConsoleConfig?.status ?: "draft"
                        ),
                        isLoading = false
                    )
                }.onFailure { error ->
                    state = state.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
            } catch (e: Exception) {
                state = state.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun updateFirebaseEnabled(enabled: Boolean) {
        state = state.copy(
            firebase = state.firebase.copy(enabled = enabled)
        )
    }

    fun updateFirebaseServiceAccount(path: String) {
        state = state.copy(
            firebase = state.firebase.copy(serviceAccountPath = path)
        )
        validateField("firebaseServiceAccount", path)
    }

    fun updateFirebaseGoogleServices(path: String) {
        state = state.copy(
            firebase = state.firebase.copy(googleServicesPath = path)
        )
    }

    fun updateFirebaseReleaseNotes(notes: String) {
        state = state.copy(
            firebase = state.firebase.copy(releaseNotes = notes)
        )
    }

    fun updateFirebaseTestGroups(groups: String) {
        state = state.copy(
            firebase = state.firebase.copy(testGroups = groups)
        )
    }

    fun updatePlayConsoleEnabled(enabled: Boolean) {
        state = state.copy(
            playConsole = state.playConsole.copy(enabled = enabled)
        )
    }

    fun updatePlayConsoleServiceAccount(path: String) {
        state = state.copy(
            playConsole = state.playConsole.copy(serviceAccountPath = path)
        )
        validateField("playConsoleServiceAccount", path)
    }

    fun updatePlayConsolePackageName(packageName: String) {
        state = state.copy(
            playConsole = state.playConsole.copy(packageName = packageName)
        )
        validateField("packageName", packageName)
    }

    fun updatePlayConsoleTrack(track: String) {
        state = state.copy(
            playConsole = state.playConsole.copy(track = track)
        )
    }

    fun updateSigningKeystorePath(path: String) {
        state = state.copy(
            signing = state.signing.copy(keystorePath = path)
        )
        validateField("keystorePath", path)
    }

    fun updateSigningStorePassword(password: String) {
        state = state.copy(
            signing = state.signing.copy(storePassword = password)
        )
    }

    fun updateSigningKeyAlias(alias: String) {
        state = state.copy(
            signing = state.signing.copy(keyAlias = alias)
        )
    }

    fun updateSigningKeyPassword(password: String) {
        state = state.copy(
            signing = state.signing.copy(keyPassword = password)
        )
    }

    private fun validateField(field: String, value: String) {
        val errors = state.validationErrors.toMutableMap()

        when (field) {
            "firebaseServiceAccount", "playConsoleServiceAccount" -> {
                if (value.isNotEmpty() && !value.endsWith(".json")) {
                    errors[field] = "Service account must be a .json file"
                } else {
                    errors.remove(field)
                }
            }
            "keystorePath" -> {
                if (value.isNotEmpty() && !value.endsWith(".jks") && !value.endsWith(".keystore")) {
                    errors[field] = "Keystore must be a .jks or .keystore file"
                } else {
                    errors.remove(field)
                }
            }
            "packageName" -> {
                if (value.isNotEmpty() && !value.matches(Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+$"))) {
                    errors[field] = "Invalid package name format"
                } else {
                    errors.remove(field)
                }
            }
        }

        state = state.copy(validationErrors = errors)
    }

    fun saveConfig() {
        if (state.validationErrors.isNotEmpty()) {
            state = state.copy(error = "Please fix validation errors before saving")
            return
        }

        state = state.copy(isSaving = true, error = null, successMessage = null)
        viewModelScope.launch {
            try {
                // Simulate save operation
                kotlinx.coroutines.delay(1000)
                state = state.copy(
                    isSaving = false,
                    successMessage = "Configuration saved successfully"
                )

                // Clear success message after 3 seconds
                kotlinx.coroutines.delay(3000)
                state = state.copy(successMessage = null)
            } catch (e: Exception) {
                state = state.copy(
                    isSaving = false,
                    error = e.message
                )
            }
        }
    }

    fun clearError() {
        state = state.copy(error = null)
    }

    fun clearSuccess() {
        state = state.copy(successMessage = null)
    }
}
