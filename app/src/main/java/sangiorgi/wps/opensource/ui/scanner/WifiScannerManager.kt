package sangiorgi.wps.opensource.ui.scanner

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sangiorgi.wps.opensource.data.database.VendorDatabaseHelper
import sangiorgi.wps.opensource.domain.models.WifiNetwork
import sangiorgi.wps.opensource.domain.models.WpsInfo
import sangiorgi.wps.opensource.utils.IwScanner
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WiFi scanner manager that provides network scanning capabilities
 */
@Singleton
class WifiScannerManager @Inject constructor(
    @param:dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
    private val vendorDatabaseHelper: VendorDatabaseHelper,
    private val iwScanner: IwScanner,
) {
    companion object {
        private const val TAG = "WifiScannerManager"
    }

    // Cache for WPS info from iw scanner. Written on IO from refreshIwWpsInfo(), read on IO from
    // getScanResults(); @Volatile guarantees readers see the latest reference.
    @Volatile
    private var iwWpsInfoCache: Map<String, WpsInfo> = emptyMap()
    private val wifiManager: WifiManager by lazy {
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    /**
     * Flow of WiFi scan results
     */
    val scanResults: Flow<List<WifiNetwork>> = callbackFlow {
        val scanReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
                    // minSdk is 24 (>= M), so EXTRA_RESULTS_UPDATED is always available
                    val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)

                    if (success) {
                        // Build results off the main thread; onReceive runs on the main thread.
                        launch(Dispatchers.IO) { trySend(getScanResults()) }
                    }
                }
            }
        }

        val intentFilter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)

        // Use ContextCompat for consistent receiver registration across API levels
        ContextCompat.registerReceiver(
            context,
            scanReceiver,
            intentFilter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        // Kick off the initial scan and emit current results, all off the main thread.
        launch(Dispatchers.IO) {
            startScan()
            trySend(getScanResults())
        }

        awaitClose {
            context.unregisterReceiver(scanReceiver)
        }
    }.distinctUntilChanged()

    /**
     * Start a WiFi scan. Suspends because it refreshes the iw WPS cache, which runs a root shell
     * command; must not be called from the main thread.
     */
    suspend fun startScan(): Boolean {
        // Check if WiFi is enabled first
        if (!wifiManager.isWifiEnabled) {
            return false
        }

        if (!hasLocationPermission()) {
            return false
        }

        // Refresh WPS info from iw when starting a new scan (requires root)
        refreshIwWpsInfo()

        @Suppress("DEPRECATION")
        return wifiManager.startScan()
    }

    /**
     * Get current scan results. Suspends because it performs SQLite vendor lookups per network;
     * must not be called from the main thread.
     */
    suspend fun getScanResults(): List<WifiNetwork> = withContext(Dispatchers.IO) {
        if (!hasLocationPermission()) {
            return@withContext emptyList()
        }

        val scanResults = try {
            @Suppress("MissingPermission")
            wifiManager.scanResults ?: emptyList()
        } catch (_: SecurityException) {
            emptyList()
        }

        scanResults.map { scanResult ->
            val bssid = scanResult.BSSID.uppercase()
            val iwWpsInfo = iwWpsInfoCache[bssid]

            if (iwWpsInfo != null) {
                Log.d(
                    TAG,
                    "Using iw WPS info for $bssid: PBC=${iwWpsInfo.isPbcSupported}, " +
                        "PIN=${iwWpsInfo.isPinSupported}, isFromIw=${iwWpsInfo.isFromIw}",
                )
            } else if (scanResult.capabilities.contains("WPS")) {
                Log.d(TAG, "No iw WPS info for $bssid, falling back to capabilities parsing")
            }

            WifiNetwork.fromScanResult(
                scanResult = scanResult,
                vendor = getVendorFromBssid(scanResult.BSSID),
                detailedWpsInfo = iwWpsInfo,
            )
        }.sortedWith(
            compareBy(
                // WPS networks first
                { !it.hasWps },
                // Then by signal strength
                { -it.signalLevel },
            ),
        )
    }

    /**
     * Refresh WPS info cache using iw scanner (requires root)
     * Called when starting a new scan
     */
    private suspend fun refreshIwWpsInfo() {
        try {
            if (iwScanner.isAvailable()) {
                iwWpsInfoCache = iwScanner.getAllWpsInfo()
                Log.d(TAG, "Updated WPS info for ${iwWpsInfoCache.size} networks from iw")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get WPS info from iw scanner", e)
        }
    }

    /**
     * Check if WiFi is enabled
     */
    fun isWifiEnabled(): Boolean = wifiManager.isWifiEnabled

    /**
     * Enable or disable WiFi
     */
    fun setWifiEnabled(enabled: Boolean): Boolean {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            @Suppress("DEPRECATION")
            wifiManager.isWifiEnabled = enabled
            true
        } else {
            // On Android Q and above, apps cannot programmatically enable/disable WiFi
            false
        }
    }

    /**
     * Get vendor from BSSID (MAC address) using vendor database
     */
    private suspend fun getVendorFromBssid(bssid: String): String {
        return vendorDatabaseHelper.getVendorByMac(bssid)
    }

    /**
     * Check if app has required permissions for WiFi scanning
     * On Android 13+, NEARBY_WIFI_DEVICES can be used instead of location
     * Note: minSdk is 24 (>= M), so runtime permissions are always required
     */
    private fun hasLocationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // On Android 13+, either NEARBY_WIFI_DEVICES or location permission works
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.NEARBY_WIFI_DEVICES,
            ) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                ) == PackageManager.PERMISSION_GRANTED
        } else {
            // On older Android, location permission is required
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ) == PackageManager.PERMISSION_GRANTED
        }
    }
}
