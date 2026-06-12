package sangiorgi.wps.opensource.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import sangiorgi.wps.opensource.R

/**
 * Comprehensive permission manager for WiFi scanning across all Android versions
 */
class PermissionManager(private val context: Context) {

    /**
     * Get all required permissions based on Android version
     */
    fun getRequiredPermissions(): List<String> {
        val permissions = mutableListOf<String>()

        // Basic WiFi permissions (all versions)
        permissions.add(Manifest.permission.ACCESS_WIFI_STATE)
        permissions.add(Manifest.permission.CHANGE_WIFI_STATE)
        permissions.add(Manifest.permission.ACCESS_NETWORK_STATE)
        permissions.add(Manifest.permission.CHANGE_NETWORK_STATE)
        permissions.add(Manifest.permission.INTERNET)

        // Location permissions for WiFi scanning
        when {
            // Android 13+ (API 33+)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                // Use NEARBY_WIFI_DEVICES if we don't need location
                permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
                // Still need location for some features
                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
                permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
            // Android 10-12 (API 29-32)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                // Fine location required for WiFi scanning
                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
                // Background location for continuous scanning (optional)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                }
            }
            // Android 8.1-9 (API 27-28)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1 -> {
                // Coarse location sufficient
                permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            // Android 7-8.0 (API 24-26) - minSdk is 24, so this is the default case
            else -> {
                // Either coarse or fine location
                permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }

        // Note: no external-storage permissions are requested. The app keeps all data
        // (databases, helper binaries, sessions) in its private internal storage.

        return permissions
    }

    /**
     * Check if WiFi scanning permissions are granted
     */
    fun hasWifiScanningPermissions(): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                // Check for NEARBY_WIFI_DEVICES or location permission
                hasPermission(Manifest.permission.NEARBY_WIFI_DEVICES) ||
                    hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                // Need fine location
                hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1 -> {
                // Need coarse or fine location
                hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ||
                    hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            else -> {
                // minSdk is 24 (>= M), so need location permission
                hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ||
                    hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    /**
     * Check if location services are enabled (required for WiFi scanning on some versions)
     */
    fun isLocationEnabled(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
            locationManager.isLocationEnabled
        } else {
            @Suppress("DEPRECATION")
            Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.LOCATION_MODE,
                Settings.Secure.LOCATION_MODE_OFF,
            ) != Settings.Secure.LOCATION_MODE_OFF
        }
    }

    /**
     * Get missing permissions
     */
    fun getMissingPermissions(): List<String> {
        return getRequiredPermissions().filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Get critical missing permissions (absolutely required for WiFi scanning)
     */
    fun getCriticalMissingPermissions(): List<String> {
        val critical = mutableListOf<String>()

        // WiFi state permissions
        if (!hasPermission(Manifest.permission.ACCESS_WIFI_STATE)) {
            critical.add(Manifest.permission.ACCESS_WIFI_STATE)
        }

        // Location permissions based on version
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                if (!hasPermission(Manifest.permission.NEARBY_WIFI_DEVICES) &&
                    !hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                ) {
                    critical.add(Manifest.permission.NEARBY_WIFI_DEVICES)
                    critical.add(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    critical.add(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }
            else -> {
                // minSdk is 24 (>= M)
                if (!hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION) &&
                    !hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                ) {
                    critical.add(Manifest.permission.ACCESS_COARSE_LOCATION)
                }
            }
        }

        return critical
    }

    /**
     * Should show rationale for permission
     * Note: minSdk is 24 (>= M), so ActivityCompat always works
     */
    fun shouldShowRationale(activity: Activity, permission: String): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Get permission rationale message
     */
    fun getPermissionRationale(context: Context, permission: String): String {
        return when (permission) {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            -> {
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
                        context.getString(R.string.rationale_location_android10)
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ->
                        context.getString(R.string.rationale_location_android8)
                    else ->
                        context.getString(R.string.rationale_location_default)
                }
            }
            Manifest.permission.NEARBY_WIFI_DEVICES ->
                context.getString(R.string.rationale_nearby_wifi)
            Manifest.permission.ACCESS_WIFI_STATE ->
                context.getString(R.string.rationale_wifi_state)
            Manifest.permission.CHANGE_WIFI_STATE ->
                context.getString(R.string.rationale_wifi_change)
            Manifest.permission.ACCESS_BACKGROUND_LOCATION ->
                context.getString(R.string.rationale_background_location)
            else -> context.getString(R.string.rationale_default)
        }
    }

    /**
     * Get Android version specific message
     */
    fun getAndroidVersionMessage(context: Context): String {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
                context.getString(R.string.android_version_13)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                context.getString(R.string.android_version_12)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
                context.getString(R.string.android_version_10)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ->
                context.getString(R.string.android_version_8)
            else ->
                context.getString(R.string.android_version_7)
        }
    }
}
