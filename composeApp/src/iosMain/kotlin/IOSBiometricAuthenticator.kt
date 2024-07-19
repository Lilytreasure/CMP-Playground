import androidx.compose.runtime.Composable
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthenticationWithBiometrics
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class IOSBiometricAuthenticator : BiometricAuthenticator {
    override suspend fun authenticate(): Boolean {
        return suspendCancellableCoroutine { continuation ->
            val context = LAContext()
            context.evaluatePolicy(
                LAPolicyDeviceOwnerAuthenticationWithBiometrics,
                localizedReason = "Authenticate using biometrics",
                reply = { success, error ->
                    if (success) {
                        if (continuation.isActive) {
                            continuation.resume(true)
                        }
                    } else {
                        val message = error?.localizedDescription ?: "Authentication failed"
                        if (continuation.isActive) {
                            continuation.resumeWithException(Exception(message))
                        }
                    }
                }
            )
        }
    }
}
object IOSPlatformContext : PlatformContext
@Composable
actual fun getPlatformContext(): PlatformContext = IOSPlatformContext

actual fun getBiometricAuthenticator(context: PlatformContext): BiometricAuthenticator = IOSBiometricAuthenticator()