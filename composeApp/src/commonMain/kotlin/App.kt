import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import multiContacts.pickMultiplatformContacts
import org.jetbrains.compose.ui.tooling.preview.Preview

enum class BioAuthNotAvailable {
    BIOAUTH_NOT_AVAILABLE
}

@Composable
@Preview
fun App() {
    val platformContext = getPlatformContext()
    val biometricAuthenticator = remember { getBiometricAuthenticator(platformContext) }
    val coroutineScope = rememberCoroutineScope()
    var isAuthenticated by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }
    val openAlertDialog = remember { mutableStateOf(false) }

    when {
        openAlertDialog.value -> {
            AlertDialogExample(
                onDismissRequest = { openAlertDialog.value = false },
                onConfirmation = {
                    openAlertDialog.value = false
                    println("Confirmation registered") // Add logic here to handle confirmation.
                },
                dialogTitle = "BioMetric Auth",
                dialogText = "Your device do not support Biometric Authentication.",
                icon = Icons.Default.Info
            )
        }
    }
    MaterialTheme {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = 20.dp, start = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(onClick = {
                coroutineScope.launch {
                    try {
                        isAuthenticated = biometricAuthenticator.authenticate()
                        authError = null
                    } catch (e: Exception) {
                        isAuthenticated = false
                        authError = e.message
                        if (e.message == BioAuthNotAvailable.BIOAUTH_NOT_AVAILABLE.toString()) {
                            openAlertDialog.value = true
                        }
                    }
                }
            }) {
                Text("Authenticate Biometric")
            }

            if (isAuthenticated) {
                Text("Authenticated successfully!")
            }

            authError?.let {
                Text("Authentication failed: $it")
            }

        }
    }
}

@Composable
fun MultiplatformContactsLoader() {
    //Android and iOS contacts
    //Library--https://github.com/Lilytreasure/MultiplatformContacts
    var phoneNumber by remember { mutableStateOf("") }
    val multiplatformContactsPicker = pickMultiplatformContacts(onResult = { number ->
        phoneNumber = number
    })

    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Button(onClick = {
            multiplatformContactsPicker.launch()
        }) {
            Text("Load contacts")
        }
        Text(text = phoneNumber)
    }
}

@Composable
fun AlertDialogExample(
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit,
    dialogTitle: String,
    dialogText: String,
    icon: ImageVector,
) {
    AlertDialog(
        icon = {
            Icon(icon, contentDescription = "Example Icon")
        },
        title = {
            Text(text = dialogTitle)
        },
        text = {
            Text(text = dialogText)
        },
        onDismissRequest = {
            onDismissRequest()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirmation()
                }
            ) {
                Text("Dismiss")
            }
        }
    )
}




