package sangiorgi.wps.opensource.ui.screens

import android.os.Parcelable
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import sangiorgi.wps.opensource.R
import sangiorgi.wps.opensource.domain.models.*
import sangiorgi.wps.opensource.utils.RootChecker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkDetailScreen(
    network: WifiNetwork,
    rootChecker: RootChecker,
    onBackClick: () -> Unit,
    onConnectionMethodSelected: (ConnectionMethod) -> Unit,
) {
    var showPinDialog by remember { mutableStateOf(false) }
    var showAdvancedOptions by remember { mutableStateOf(false) }
    var selectedMethod by remember { mutableStateOf<ConnectionMethod?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Check root status asynchronously to avoid blocking UI
    var isRooted by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        // Check root on IO dispatcher to avoid blocking main thread
        withContext(Dispatchers.IO) {
            isRooted = rootChecker.isRootAvailable()
        }
    }

    val rootRequiredMessage = stringResource(R.string.root_required_message)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.network_details)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            // Network Info Card
            NetworkInfoCard(network)

            Spacer(modifier = Modifier.height(16.dp))

            // WPS Status Card
            WpsStatusCard(network)

            Spacer(modifier = Modifier.height(16.dp))

            // Root Status Card (show warning if not rooted)
            if (!isRooted) {
                RootRequiredCard()
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Connection Methods
            Text(
                text = stringResource(R.string.wps_connection_methods),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Standard WPS Connection
            ConnectionMethodCard(
                title = stringResource(R.string.standard_wps),
                description = stringResource(R.string.standard_wps_description),
                icon = Icons.Default.Pin,
                enabled = network.hasWps && !network.isWpsLocked,
                isRooted = isRooted,
                onClick = {
                    if (isRooted) {
                        selectedMethod = ConnectionMethod.STANDARD
                        showPinDialog = true
                    } else {
                        scope.launch {
                            snackbarHostState.showSnackbar(rootRequiredMessage)
                        }
                    }
                },
            )

            // Pixie Dust Attack
            ConnectionMethodCard(
                title = stringResource(R.string.pixie_dust),
                description = stringResource(R.string.pixie_dust_description),
                icon = Icons.Default.Stars,
                enabled = network.hasWps,
                isRooted = isRooted,
                onClick = {
                    if (isRooted) {
                        onConnectionMethodSelected(ConnectionMethod.PIXIE_DUST)
                    } else {
                        scope.launch {
                            snackbarHostState.showSnackbar(rootRequiredMessage)
                        }
                    }
                },
            )

            // Belkin Method
            if (network.vendor.contains("Belkin", ignoreCase = true)) {
                ConnectionMethodCard(
                    title = stringResource(R.string.belkin_specific),
                    description = stringResource(R.string.belkin_specific_description),
                    icon = Icons.Default.Router,
                    enabled = network.hasWps,
                    isRooted = isRooted,
                    onClick = {
                        if (isRooted) {
                            onConnectionMethodSelected(ConnectionMethod.BELKIN)
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar(rootRequiredMessage)
                            }
                        }
                    },
                )
            }

            // Show Advanced Options
            TextButton(
                onClick = { showAdvancedOptions = !showAdvancedOptions },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = if (showAdvancedOptions) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                )
                Text(stringResource(R.string.advanced_options))
            }

            AnimatedVisibility(visible = showAdvancedOptions) {
                Column {
                    // Brute Force
                    ConnectionMethodCard(
                        title = stringResource(R.string.brute_force),
                        description = stringResource(R.string.brute_force_description),
                        icon = Icons.Default.AllInclusive,
                        enabled = network.hasWps,
                        isRooted = isRooted,
                        isAdvanced = true,
                        onClick = {
                            if (isRooted) {
                                onConnectionMethodSelected(ConnectionMethod.BRUTE_FORCE)
                            } else {
                                scope.launch {
                                    snackbarHostState.showSnackbar(rootRequiredMessage)
                                }
                            }
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Warning Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                ),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.warning_legal),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }
    }

    // PIN Selection Dialog
    if (showPinDialog && isRooted) {
        PinSelectionDialog(
            bssid = network.bssid,
            ssid = network.ssid,
            onDismiss = { showPinDialog = false },
            onPinSelected = { pins ->
                showPinDialog = false
                if (selectedMethod == ConnectionMethod.STANDARD) {
                    onConnectionMethodSelected(ConnectionMethod.STANDARD_WITH_PINS(pins))
                }
            },
        )
    }
}

@Composable
private fun RootRequiredCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(32.dp),
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = stringResource(R.string.root_required_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                Text(
                    text = stringResource(R.string.root_required_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    }
}

@Composable
private fun NetworkInfoCard(network: WifiNetwork) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = network.ssid,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(8.dp))

            InfoRow(stringResource(R.string.label_bssid), network.bssid)
            InfoRow(
                stringResource(R.string.label_signal),
                stringResource(R.string.signal_strength_format, network.signalLevel, network.signalStrength.name),
            )
            InfoRow(
                stringResource(R.string.label_channel),
                stringResource(R.string.channel_frequency_format, network.channel, network.frequency),
            )
            InfoRow(stringResource(R.string.label_security), network.security.name)
            InfoRow(stringResource(R.string.label_vendor), network.vendor)
            InfoRow(
                stringResource(R.string.label_distance),
                stringResource(R.string.distance_meters_format, network.distance),
            )
            InfoRow(
                stringResource(R.string.label_band),
                when (network.band) {
                    WifiBand.BAND_2_4_GHZ -> stringResource(R.string.band_2_4_ghz)
                    WifiBand.BAND_5_GHZ -> stringResource(R.string.band_5_ghz)
                    WifiBand.BAND_6_GHZ -> stringResource(R.string.band_6_ghz)
                    else -> stringResource(R.string.unknown)
                },
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun WpsStatusCard(network: WifiNetwork) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                !network.hasWps -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                network.isWpsLocked -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.primaryContainer
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = when {
                    !network.hasWps -> Icons.Default.Cancel
                    network.isWpsLocked -> Icons.Default.Lock
                    else -> Icons.Default.CheckCircle
                },
                contentDescription = null,
                tint = when {
                    !network.hasWps -> MaterialTheme.colorScheme.error
                    network.isWpsLocked -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.primary
                },
                modifier = Modifier.size(32.dp),
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = when {
                        !network.hasWps -> stringResource(R.string.wps_not_available)
                        network.isWpsLocked -> stringResource(R.string.wps_locked)
                        else -> stringResource(R.string.wps_available)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )

                network.wpsInfo?.let { wpsInfo ->
                    val methods = mutableListOf<String>()
                    if (wpsInfo.isPbcSupported) methods.add(stringResource(R.string.method_pbc))
                    if (wpsInfo.isPinSupported) methods.add(stringResource(R.string.method_pin))
                    when {
                        methods.isNotEmpty() -> {
                            Text(
                                text = stringResource(R.string.supported_methods, methods.joinToString(", ")),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        wpsInfo.isFromIw -> {
                            // Got data from iw but no methods detected (unusual)
                            Text(
                                text = stringResource(R.string.wps_no_methods_detected),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        wpsInfo.isEnabled -> {
                            // WPS is available but we don't know the methods without root/iw
                            Text(
                                text = stringResource(R.string.wps_methods_unknown),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConnectionMethodCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean = true,
    isRooted: Boolean = true,
    isAdvanced: Boolean = false,
    onClick: () -> Unit,
) {
    // The card is clickable even when not rooted to show the snackbar
    // But visually it appears disabled when not rooted
    val visuallyEnabled = enabled && isRooted

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onClick,
        // Keep enabled to allow click for snackbar
        enabled = enabled,
        colors = CardDefaults.cardColors(
            containerColor = when {
                !visuallyEnabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                isAdvanced -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                else -> MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (visuallyEnabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                },
                modifier = Modifier.size(24.dp),
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = if (visuallyEnabled) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        },
                    )
                    if (!isRooted) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        )
                    }
                }
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (visuallyEnabled) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    },
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = if (visuallyEnabled) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                },
            )
        }
    }
}

sealed class ConnectionMethod : Parcelable {
    @Parcelize object STANDARD : ConnectionMethod()

    @Parcelize object PIXIE_DUST : ConnectionMethod()

    @Parcelize object BELKIN : ConnectionMethod()

    @Parcelize object BRUTE_FORCE : ConnectionMethod()

    @Parcelize object CUSTOM_PIN : ConnectionMethod()

    @Parcelize data class STANDARD_WITH_PINS(val pins: List<String>) : ConnectionMethod()

    @Parcelize data class CUSTOM_PIN_WITH_VALUE(val pin: String) : ConnectionMethod()
}
