package com.jayanth.hearit.security

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

sealed interface BiometricAuthResult {
    data object Success : BiometricAuthResult
    data object Cancelled : BiometricAuthResult
    data class Failed(val error: String) : BiometricAuthResult
    data class Unavailable(val reason: String) : BiometricAuthResult
}

/**
 * Manages biometric and device credential (PIN/pattern/password) authentication
 * for securing sensitive settings and notification history (Master Prompt §15).
 */
@Singleton
class BiometricAuthManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val keyguardManager: KeyguardManager? by lazy {
        context.getSystemService(KeyguardManager::class.java)
    }

    private val biometricManager: BiometricManager? by lazy {
        context.getSystemService(BiometricManager::class.java)
    }

    /**
     * Checks whether the device has a secure lock screen (PIN/pattern/password)
     * or biometric enrolled that can satisfy authentication.
     */
    fun canAuthenticate(): Boolean {
        return try {
            val km = keyguardManager ?: return false
            if (!km.isDeviceSecure) return false

            val bm = biometricManager ?: return true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val status = bm.canAuthenticate(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL,
                )
                status == BiometricManager.BIOMETRIC_SUCCESS
            } else {
                km.isDeviceSecure
            }
        } catch (e: Throwable) {
            false
        }
    }

    /**
     * Launches the system Biometric / Device Credential prompt.
     */
    fun authenticate(
        activity: Activity,
        title: String,
        subtitle: String = "",
        onResult: (BiometricAuthResult) -> Unit,
    ): CancellationSignal {
        val cancellationSignal = CancellationSignal()

        if (!canAuthenticate()) {
            onResult(
                BiometricAuthResult.Unavailable(
                    "No biometric or device lock (PIN/pattern/password) is set on this device.",
                ),
            )
            return cancellationSignal
        }

        try {
            val builder = BiometricPrompt.Builder(activity)
                .setTitle(title)
                .apply {
                    if (subtitle.isNotBlank()) setSubtitle(subtitle)
                }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                builder.setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL,
                )
            } else {
                @Suppress("DEPRECATION")
                builder.setDeviceCredentialAllowed(true)
            }

            val prompt = builder.build()
            prompt.authenticate(
                cancellationSignal,
                activity.mainExecutor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                        onResult(BiometricAuthResult.Success)
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                        if (errorCode == BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED ||
                            errorCode == BiometricPrompt.BIOMETRIC_ERROR_CANCELED
                        ) {
                            onResult(BiometricAuthResult.Cancelled)
                        } else {
                            onResult(BiometricAuthResult.Failed(errString?.toString() ?: "Authentication failed"))
                        }
                    }

                    override fun onAuthenticationFailed() {
                        // Soft failure — Android framework keeps prompt open for retry
                    }
                },
            )
        } catch (e: Exception) {
            onResult(BiometricAuthResult.Failed(e.message ?: "Failed to launch authentication prompt"))
        }

        return cancellationSignal
    }
}
