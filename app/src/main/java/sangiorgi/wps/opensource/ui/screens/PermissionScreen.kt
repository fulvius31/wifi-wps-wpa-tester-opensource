package sangiorgi.wps.opensource.ui.screens

import android.Manifest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import sangiorgi.wps.opensource.R
import sangiorgi.wps.opensource.permissions.PermissionManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionScreen(
    onPermissionsGranted: () -> Unit,
    onRequestPermissions: (List<String>) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    val permissionManager = remember { PermissionManager(context) }
    var showDetails by remember { mutableStateOf(false) }

    val missingPermissions = remember { permissionManager.getMissingPermissions() }
    val criticalPermissions = remember { permissionManager.getCriticalMissingPermissions() }
    val hasWifiPermissions = remember { permissionManager.hasWifiScanningPermissions() }
    val isLocationEnabled = remember { permissionManager.isLocationEnabled() }

    LaunchedEffect(hasWifiPermissions) {
        if (hasWifiPermissions && isLocationEnabled) {
            onPermissionsGranted()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.permissions_required)) },
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
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Main icon
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Title
            Text(
                text = stringResource(R.string.wifi_scanner_permissions),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Android version message
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Android,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = permissionManager.getAndroidVersionMessage(context),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Location services warning if needed (minSdk is 24 >= M, so always required)
            if (!isLocationEnabled) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.location_services_disabled),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                            Text(
                                text = stringResource(R.string.enable_location_for_wifi),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Permission list
            Text(
                text = stringResource(R.string.required_permissions),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Critical permissions
            criticalPermissions.forEach { permission ->
                PermissionItem(
                    permission = permission,
                    isCritical = true,
                    rationale = permissionManager.getPermissionRationale(context, permission),
                )
            }

            // Show more button
            if (missingPermissions.size > criticalPermissions.size) {
                TextButton(
                    onClick = { showDetails = !showDetails },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = if (showDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                    )
                    Text(
                        if (showDetails) {
                            stringResource(R.string.hide_additional_permissions)
                        } else {
                            stringResource(R.string.show_additional_permissions)
                        },
                    )
                }
            }

            // Additional permissions
            AnimatedVisibility(visible = showDetails) {
                Column {
                    missingPermissions
                        .filter { it !in criticalPermissions }
                        .forEach { permission ->
                            PermissionItem(
                                permission = permission,
                                isCritical = false,
                                rationale = permissionManager.getPermissionRationale(context, permission),
                            )
                        }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action buttons
            Button(
                onClick = {
                    if (criticalPermissions.isNotEmpty()) {
                        onRequestPermissions(criticalPermissions)
                    } else {
                        onRequestPermissions(missingPermissions)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = missingPermissions.isNotEmpty(),
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.grant_permissions))
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Settings, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.open_app_settings))
            }

            // Info card
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.permission_info_message),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionItem(permission: String, isCritical: Boolean, rationale: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCritical) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = getPermissionIcon(permission),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = if (isCritical) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = getPermissionName(permission),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = rationale,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (isCritical) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.error,
                    ) {
                        Text(
                            text = stringResource(R.string.required),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onError,
                        )
                    }
                }
            }
        }
    }
}

private fun getPermissionIcon(permission: String): ImageVector {
    return when (permission) {
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        -> Icons.Default.LocationOn
        Manifest.permission.NEARBY_WIFI_DEVICES,
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.CHANGE_WIFI_STATE,
        -> Icons.Default.Wifi
        else -> Icons.Default.Security
    }
}

@Composable
private fun getPermissionName(permission: String): String {
    return when (permission) {
        Manifest.permission.ACCESS_FINE_LOCATION -> stringResource(R.string.permission_precise_location)
        Manifest.permission.ACCESS_COARSE_LOCATION -> stringResource(R.string.permission_approximate_location)
        Manifest.permission.ACCESS_BACKGROUND_LOCATION -> stringResource(R.string.permission_background_location)
        Manifest.permission.NEARBY_WIFI_DEVICES -> stringResource(R.string.permission_nearby_wifi_devices)
        Manifest.permission.ACCESS_WIFI_STATE -> stringResource(R.string.permission_wifi_state)
        Manifest.permission.CHANGE_WIFI_STATE -> stringResource(R.string.permission_change_wifi_state)
        Manifest.permission.ACCESS_NETWORK_STATE -> stringResource(R.string.permission_network_state)
        Manifest.permission.CHANGE_NETWORK_STATE -> stringResource(R.string.permission_change_network_state)
        Manifest.permission.INTERNET -> stringResource(R.string.permission_internet_access)
        else -> permission.substringAfterLast(".")
    }
}
