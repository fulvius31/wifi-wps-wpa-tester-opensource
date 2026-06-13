package sangiorgi.wps.opensource.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import sangiorgi.wps.opensource.R
import sangiorgi.wps.opensource.domain.models.WifiNetwork
import sangiorgi.wps.opensource.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionProgressScreen(
    network: WifiNetwork,
    connectionMethod: ConnectionMethod,
    connectionState: ConnectionState,
    onCancel: () -> Unit,
    onClose: () -> Unit,
    onRetry: () -> Unit,
    onDone: () -> Unit,
) {
    val listState = rememberLazyListState()
    val context = LocalContext.current

    LaunchedEffect(connectionState.logs) {
        if (connectionState.logs.isNotEmpty()) {
            listState.animateScrollToItem(connectionState.logs.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.wps_connection)) },
                navigationIcon = {
                    IconButton(
                        onClick = onClose,
                        enabled = connectionState.status != ConnectionStatus.CONNECTING,
                    ) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            // Connection Status Card
            ConnectionStatusCard(
                network = network,
                connectionState = connectionState,
                connectionMethod = connectionMethod,
            )

            // Progress Section
            if (connectionState.totalPins > 0) {
                LinearProgressIndicator(
                    progress = {
                        connectionState.currentPinIndex.toFloat() / connectionState.totalPins
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )

                Text(
                    text = stringResource(
                        R.string.testing_pin_progress,
                        connectionState.currentPinIndex + 1,
                        connectionState.totalPins,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    textAlign = TextAlign.Center,
                )
            }

            // Logs Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black,
                ),
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    items(connectionState.logs) { log ->
                        LogEntry(log)
                    }
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                when (connectionState.status) {
                    ConnectionStatus.CONNECTING -> {
                        Button(
                            onClick = onCancel,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                            ),
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.stop))
                        }
                    }
                    ConnectionStatus.SUCCESS -> {
                        Button(
                            onClick = onDone,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                            ),
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.done))
                        }
                    }
                    ConnectionStatus.FAILED, ConnectionStatus.CANCELLED -> {
                        OutlinedButton(
                            onClick = onClose,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.close))
                        }
                        Button(
                            onClick = onRetry,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.retry))
                        }
                    }
                    ConnectionStatus.WIFI_ENABLED -> {
                        OutlinedButton(
                            onClick = { openWifiSettings(context) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.Wifi, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.open_wifi_settings))
                        }
                        Button(
                            onClick = onRetry,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.retry))
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}

@Composable
private fun ConnectionStatusCard(
    network: WifiNetwork,
    connectionState: ConnectionState,
    connectionMethod: ConnectionMethod,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (connectionState.status) {
                ConnectionStatus.SUCCESS -> MaterialTheme.colorScheme.primaryContainer
                ConnectionStatus.FAILED, ConnectionStatus.WIFI_ENABLED -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Status Icon with Animation
            Box(
                modifier = Modifier.size(64.dp),
                contentAlignment = Alignment.Center,
            ) {
                when (connectionState.status) {
                    ConnectionStatus.CONNECTING -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(64.dp),
                            strokeWidth = 4.dp,
                        )
                    }
                    ConnectionStatus.SUCCESS -> {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    ConnectionStatus.FAILED -> {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                    ConnectionStatus.CANCELLED -> {
                        Icon(
                            imageVector = Icons.Default.Cancel,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    ConnectionStatus.WIFI_ENABLED -> {
                        Icon(
                            imageVector = Icons.Default.WifiOff,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                    else -> {}
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = network.ssid,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text = getMethodDescription(connectionMethod),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Current Status
            Text(
                text = when (connectionState.status) {
                    ConnectionStatus.CONNECTING ->
                        stringResource(R.string.testing_pin, connectionState.currentPin)
                    ConnectionStatus.SUCCESS -> stringResource(R.string.connected_successfully)
                    ConnectionStatus.FAILED ->
                        connectionState.errorMessage ?: stringResource(R.string.connection_failed)
                    ConnectionStatus.CANCELLED -> stringResource(R.string.connection_cancelled)
                    ConnectionStatus.WIFI_ENABLED -> stringResource(R.string.wifi_must_be_disabled)
                    else -> stringResource(R.string.initializing)
                },
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )

            // Success Details
            if (connectionState.status == ConnectionStatus.SUCCESS && connectionState.successPin != null) {
                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primary,
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = stringResource(R.string.pin_found),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Text(
                            text = connectionState.successPin,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontFamily = FontFamily.Monospace,
                        )
                        if (connectionState.password != null) {
                            Text(
                                text = stringResource(R.string.password_format, connectionState.password),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        ResultActions(
                            ssid = network.ssid,
                            pin = connectionState.successPin,
                            password = connectionState.password,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LogEntry(log: ConnectionLog) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = log.timestamp,
            style = MaterialTheme.typography.bodySmall,
            color = Neutral60,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(60.dp),
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = log.message,
            style = MaterialTheme.typography.bodySmall,
            color = when (log.type) {
                LogType.SUCCESS -> AttackSuccess
                LogType.ERROR -> AttackFailed
                LogType.WARNING -> AttackWarning
                LogType.INFO -> Neutral90
                LogType.DEBUG -> Neutral60
            },
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
private fun getMethodDescription(method: ConnectionMethod): String {
    return when (method) {
        is ConnectionMethod.STANDARD -> stringResource(R.string.method_standard_wps_attack)
        is ConnectionMethod.STANDARD_WITH_PINS -> stringResource(R.string.method_testing_pins, method.pins.size)
        is ConnectionMethod.PIXIE_DUST -> stringResource(R.string.method_pixie_dust_attack)
        is ConnectionMethod.BELKIN -> stringResource(R.string.method_belkin_attack)
        is ConnectionMethod.BRUTE_FORCE -> stringResource(R.string.method_brute_force_attack)
        is ConnectionMethod.CUSTOM_PIN -> stringResource(R.string.method_custom_pin_test)
        is ConnectionMethod.CUSTOM_PIN_WITH_VALUE -> stringResource(R.string.method_testing_pin_value, method.pin)
    }
}

@Composable
private fun ResultActions(ssid: String, pin: String, password: String?) {
    val context = LocalContext.current
    val copiedMessage = stringResource(R.string.result_copied)
    val chooserTitle = stringResource(R.string.share_chooser_title)
    val resultText = buildResultText(context, ssid, pin, password)

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(
            onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("WPS result", resultText))
                Toast.makeText(context, copiedMessage, Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onPrimary),
        ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(stringResource(R.string.action_copy))
        }
        TextButton(
            onClick = {
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, resultText)
                }
                context.startActivity(Intent.createChooser(sendIntent, chooserTitle))
            },
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onPrimary),
        ) {
            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(stringResource(R.string.action_share))
        }
    }
}

private fun openWifiSettings(context: Context) {
    // On Android 10+ the slide-up WiFi panel lets the user toggle WiFi off without leaving the
    // app; older versions fall back to the full WiFi settings screen.
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        Intent(Settings.Panel.ACTION_WIFI)
    } else {
        Intent(Settings.ACTION_WIFI_SETTINGS)
    }
    try {
        context.startActivity(intent)
    } catch (e: android.content.ActivityNotFoundException) {
        // Extremely rare (a ROM with no WiFi settings activity); fall back to general settings.
        Log.w("ConnectionProgressScreen", "No WiFi settings activity", e)
        runCatching { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }
    }
}

private fun buildResultText(context: Context, ssid: String, pin: String, password: String?): String {
    return buildString {
        append(context.getString(R.string.share_result_format, ssid, pin))
        if (!password.isNullOrEmpty()) {
            append("\n")
            append(context.getString(R.string.password_format, password))
        }
    }
}

data class ConnectionState(
    val status: ConnectionStatus = ConnectionStatus.IDLE,
    val currentPin: String = "",
    val currentPinIndex: Int = 0,
    val totalPins: Int = 0,
    val successPin: String? = null,
    val password: String? = null,
    val errorMessage: String? = null,
    val logs: List<ConnectionLog> = emptyList(),
)

enum class ConnectionStatus {
    IDLE,
    CONNECTING,
    SUCCESS,
    FAILED,
    CANCELLED,

    // WiFi is on, so the system wpa_supplicant holds wlan0; the attempt can't start until the
    // user disables WiFi.
    WIFI_ENABLED,
}

data class ConnectionLog(
    val timestamp: String,
    val message: String,
    val type: LogType,
)

enum class LogType {
    SUCCESS,
    ERROR,
    WARNING,
    INFO,
    DEBUG,
}
