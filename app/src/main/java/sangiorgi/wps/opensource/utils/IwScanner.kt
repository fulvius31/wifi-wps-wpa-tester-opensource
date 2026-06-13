package sangiorgi.wps.opensource.utils

import android.content.Context
import android.util.Log
import com.topjohnwu.superuser.Shell
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import sangiorgi.wps.opensource.domain.models.WpsInfo
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Scanner that uses the `iw` binary to get detailed WPS information
 * from wireless networks. Requires root access.
 */
@Singleton
class IwScanner @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val rootChecker: RootChecker,
) {
    companion object {
        private const val TAG = "IwScanner"
        private const val IW_BINARY = "iw"
    }

    private val filesDir: File = context.filesDir
    private val iwPath: String = File(filesDir, IW_BINARY).absolutePath

    /**
     * Check if the iw binary is available and executable (quick synchronous check).
     * Does NOT check root - use isFullyAvailable() for that.
     */
    fun isBinaryAvailable(): Boolean {
        val iwFile = File(iwPath)
        val fileExists = iwFile.exists() && iwFile.canExecute()
        if (!fileExists) {
            Log.d(TAG, "iw binary not found or not executable at $iwPath")
            return false
        }
        return true
    }

    /**
     * Quick check using cached root status. Returns false if root hasn't been checked yet.
     * Use isFullyAvailable() for a definitive check that includes root request.
     */
    fun isAvailable(): Boolean {
        if (!isBinaryAvailable()) return false

        val cachedRoot = rootChecker.getCachedRootStatus()
        if (cachedRoot == null || !cachedRoot) {
            Log.d(TAG, "Root not available (cached: $cachedRoot), iw scanner disabled")
            return false
        }
        return true
    }

    /**
     * Check if the iw binary is available, executable, AND root is available.
     * This is a suspend function that may trigger a root request.
     */
    suspend fun isFullyAvailable(): Boolean {
        if (!isBinaryAvailable()) return false

        val hasRoot = rootChecker.isRootAvailable()
        if (!hasRoot) {
            Log.d(TAG, "Root not available, iw scanner disabled")
            return false
        }
        return true
    }

    /**
     * Get the wireless interface name (usually wlan0)
     */
    suspend fun getWirelessInterface(): String? = withContext(Dispatchers.IO) {
        try {
            val result = Shell.cmd("ls /sys/class/net/ | grep -E '^wlan'").exec()
            if (result.isSuccess && result.out.isNotEmpty()) {
                result.out.firstOrNull()?.trim()
            } else {
                // Fallback to wlan0
                "wlan0"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get wireless interface", e)
            "wlan0"
        }
    }

    /**
     * Get WPS information for all visible networks
     */
    suspend fun getAllWpsInfo(): Map<String, WpsInfo> = withContext(Dispatchers.IO) {
        if (!isFullyAvailable()) {
            Log.w(TAG, "iw binary not available or no root")
            return@withContext emptyMap()
        }

        val iface = getWirelessInterface() ?: return@withContext emptyMap()
        val libPath = filesDir.absolutePath

        try {
            // Run iw scan dump with proper library path (same pattern as wpa_supplicant)
            val cmd = "cd $libPath && export LD_LIBRARY_PATH=$libPath && ./$IW_BINARY dev $iface scan dump"
            Log.d(TAG, "Running iw command: $cmd")
            val result = Shell.cmd(cmd).exec()

            if (!result.isSuccess) {
                Log.e(TAG, "iw scan dump failed: ${result.err.joinToString("\n")}")
                return@withContext emptyMap()
            }

            Log.d(TAG, "iw scan dump returned ${result.out.size} lines")
            if (result.out.isEmpty()) {
                Log.w(TAG, "iw scan dump returned empty output")
            } else {
                // Log first few lines to debug
                result.out.take(10).forEach { Log.d(TAG, "iw output: $it") }
            }

            IwScanResultParser.parse(result.out)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get all WPS info", e)
            emptyMap()
        }
    }
}
