package org.example.project

import BiometricAuthenticator
import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

enum class BioAuthNotAvailable{
    BIOAUTH_NOT_AVAILABLE
}

class AndroidBiometricAuthenticator(private val context: Context) : BiometricAuthenticator {
    override suspend fun authenticate(): Boolean {
        val biometricManager = BiometricManager.from(context)
        if (biometricManager.canAuthenticate() != BiometricManager.BIOMETRIC_SUCCESS) {
            throw Exception(BioAuthNotAvailable.BIOAUTH_NOT_AVAILABLE.toString())
        }

        return suspendCancellableCoroutine { continuation ->
            val executor = ContextCompat.getMainExecutor(context)
            val biometricPrompt = BiometricPrompt(context as FragmentActivity,
                executor, object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        if (continuation.isActive) {
                            continuation.resume(true)
                        }
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        if (continuation.isActive) {
                            continuation.resumeWithException(Exception(errString.toString()))
                        }
                    }

                    override fun onAuthenticationFailed() {
                        if (continuation.isActive) {
                            continuation.resume(false)
                        }
                    }
                })

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric Authentication")
                .setSubtitle("Authenticate using biometrics")
                .setNegativeButtonText("Cancel")
                .build()

            biometricPrompt.authenticate(promptInfo)
        }
    }
}



