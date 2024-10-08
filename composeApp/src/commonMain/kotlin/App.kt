import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import multiContacts.pickMultiplatformContacts
import multiplatformWebView.WebViewEngine
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.math.roundToInt

enum class BioAuthNotAvailable {
    BIOAUTH_NOT_AVAILABLE
}

val dataStore = createDataStore()

@Composable
@Preview
fun App() {
    MaterialTheme {
        MultiplatformWebView()
    }
}

@Composable
fun BioauthView() {
    val scope = rememberCoroutineScope()
    val platformContext = getPlatformContext()
    val biometricAuthenticator = remember { getBiometricAuthenticator(platformContext) }
    val coroutineScope = rememberCoroutineScope()
    var isAuthenticated by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }
    val dataStoreExample = stringPreferencesKey("data_store")
    var dataStoreValue by remember { mutableStateOf("") }
    var checked by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        dataStore.data.map { preferences ->
            preferences[dataStoreExample] ?: ""
        }.collectLatest {
            dataStoreValue = it
            checked = dataStoreValue == "Enabled"
        }
    }
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
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 20.dp, start = 16.dp, end = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        println("Data Store value is $dataStoreValue")
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Enable Biometric")
                }
                Switch(
                    checked = checked,
                    onCheckedChange = { checkedValue ->
                        checked = checkedValue
                        val newValue = if (checked) "Enabled" else "Disabled"
                        scope.launch {
                            dataStore.edit {
                                it[dataStoreExample] = newValue
                            }
                        }
                    },
                    thumbContent = if (checked) {
                        {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(SwitchDefaults.IconSize),
                            )
                        }
                    } else {
                        null
                    }
                )
                Text(
                    "DataStore value is: $dataStoreValue",
                    modifier = Modifier.padding(vertical = 18.dp)
                )

                if (checked) {
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

@Composable
fun MultiplatformWebView() {
    var isLoadingDescription by remember { mutableStateOf(false) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    Scaffold(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .padding(it)
                .fillMaxSize()
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .windowInsetsPadding(WindowInsets.ime)
            ) {
                WebViewEngine(
                    url = "https://github.com/Lilytreasure",
                    isLoading = { loadDelegate ->
                        isLoadingDescription = loadDelegate
                    },
                    onUrlClicked = { url -> },
                    onCreated = {},
                    onDispose = {}
                )
                if (isLoadingDescription) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                    )
                }
                if (isLoadingDescription) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.Center)
                    )
                }
                //Enabled dragging the dismissing button to not get in the way of the WebView content
                IconButton(
                    onClick = {
                        // Handle onClick
                    },
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.onBackground,
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume() // Consume the gesture to prevent interference with other gestures
                                offsetX += dragAmount.x
                                offsetY += dragAmount.y
                            }
                        }
                ) {
                    Icon(
                        Icons.Filled.Cancel,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        }
    }
}




