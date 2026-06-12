package sangiorgi.wps.opensource.data.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import sangiorgi.wps.lib.assets.WpaToolsPaths
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class for accessing the PIN database.
 * The database contains known default/static PINs for routers based on their MAC prefix.
 *
 * MAC prefix is the first 3 octets (6 hex characters) of the BSSID,
 * e.g., "00:11:22:33:44:55" -> "001122"
 */
@Singleton
class PinDatabaseHelper @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    companion object {
        private const val TAG = "PinDatabaseHelper"
        private const val TABLE_NAME = "pins"
        private const val COLUMN_MAC = "MAC"
        private const val COLUMN_PIN = "pin"
        private const val MAX_PINS_PER_MAC = 8
    }

    @Volatile
    private var database: SQLiteDatabase? = null
    private val databasePath: String = WpaToolsPaths(context).getPinDatabasePath()

    /**
     * Lazily open the database on first use. Synchronized and idempotent so the disk I/O happens
     * off the main thread (from the suspend query) and only once even under concurrent first
     * access. Called from getPinsByMac, never from init, to avoid blocking the thread that
     * constructs this singleton.
     */
    @Synchronized
    private fun ensureDatabaseOpen() {
        if (database?.isOpen == true) return
        try {
            database = SQLiteDatabase.openDatabase(
                databasePath,
                null,
                SQLiteDatabase.OPEN_READONLY,
            )
            Log.d(TAG, "PIN database opened successfully from: $databasePath")
        } catch (e: SQLiteException) {
            Log.e(TAG, "Error opening PIN database at: $databasePath", e)
        }
    }

    /**
     * Get static/default PINs for a given MAC address.
     * Looks up by the first 3 octets (6 hex characters) of the MAC.
     *
     * @param macAddress The router's BSSID (e.g., "00:11:22:33:44:55")
     * @return List of known static PINs for this MAC prefix, empty if none found
     */
    suspend fun getPinsByMac(macAddress: String): List<String> = withContext(Dispatchers.IO) {
        ensureDatabaseOpen()

        val normalizedMac = normalizeMacAddress(macAddress)
        if (normalizedMac.length < 6) {
            Log.w(TAG, "Invalid MAC address: $macAddress")
            return@withContext emptyList()
        }

        val macPrefix = normalizedMac.substring(0, 6).lowercase()

        try {
            database?.let { db ->
                val cursor = db.rawQuery(
                    "SELECT $COLUMN_PIN FROM $TABLE_NAME WHERE $COLUMN_MAC = ? LIMIT $MAX_PINS_PER_MAC",
                    arrayOf(macPrefix),
                )

                val pins = mutableListOf<String>()
                cursor.use {
                    while (it.moveToNext()) {
                        val pin = it.getString(0)
                        if (!pin.isNullOrEmpty()) {
                            pins.add(pin)
                        }
                    }
                }

                if (pins.isNotEmpty()) {
                    Log.d(TAG, "Found ${pins.size} static PINs for MAC prefix: $macPrefix")
                }

                pins
            } ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error querying PINs for MAC: $macAddress", e)
            emptyList()
        }
    }

    private fun normalizeMacAddress(mac: String): String {
        return mac.uppercase().replace(":", "").replace("-", "")
    }
}
