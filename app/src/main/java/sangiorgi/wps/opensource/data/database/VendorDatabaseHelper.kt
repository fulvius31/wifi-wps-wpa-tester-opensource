package sangiorgi.wps.opensource.data.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VendorDatabaseHelper @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    companion object {
        private const val TAG = "VendorDatabaseHelper"
        private const val TABLE_NAME = "oui"
        private const val COLUMN_MAC = "mac"
        private const val COLUMN_VENDOR = "vendor"
        private const val DATABASE_FILE = "vendor.db"
    }

    @Volatile
    private var database: SQLiteDatabase? = null
    private val databasePath: String = File(context.filesDir, DATABASE_FILE).absolutePath

    /**
     * Lazily extract the bundled DB and open it on first use. Synchronized and idempotent so the
     * disk I/O happens off the main thread (from the suspend query) and only once even under
     * concurrent first access. Called from getVendorByMac, never from init, to avoid blocking the
     * thread that constructs this singleton.
     */
    @Synchronized
    private fun ensureDatabaseOpen() {
        if (database?.isOpen == true) return
        extractDatabaseIfNeeded()
        openDatabase()
    }

    private fun extractDatabaseIfNeeded() {
        val dbFile = File(databasePath)
        if (dbFile.exists()) return

        try {
            context.assets.open(DATABASE_FILE).use { input ->
                FileOutputStream(dbFile).use { output ->
                    input.copyTo(output)
                }
            }
            Log.d(TAG, "Extracted $DATABASE_FILE to: $databasePath")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract $DATABASE_FILE", e)
        }
    }

    private fun openDatabase() {
        try {
            database = SQLiteDatabase.openDatabase(
                databasePath,
                null,
                SQLiteDatabase.OPEN_READONLY,
            )
            Log.d(TAG, "Database opened successfully from: $databasePath")
        } catch (e: SQLiteException) {
            Log.e(TAG, "Error opening database at: $databasePath", e)
        }
    }

    suspend fun getVendorByMac(macAddress: String): String = withContext(Dispatchers.IO) {
        ensureDatabaseOpen()

        val normalizedMac = normalizeMacAddress(macAddress)
        val macPrefix = extractMacPrefix(normalizedMac)

        try {
            database?.let { db ->
                val cursor = db.rawQuery(
                    "SELECT $COLUMN_VENDOR FROM $TABLE_NAME WHERE $COLUMN_MAC = ? LIMIT 1",
                    arrayOf(macPrefix),
                )

                cursor.use {
                    if (it.moveToFirst()) {
                        it.getString(0) ?: "Unknown"
                    } else {
                        "Unknown"
                    }
                }
            } ?: "Unknown"
        } catch (e: Exception) {
            Log.e(TAG, "Error querying vendor for MAC: $macAddress", e)
            "Unknown"
        }
    }

    private fun normalizeMacAddress(mac: String): String {
        return mac.uppercase().replace(":", "").replace("-", "")
    }

    private fun extractMacPrefix(normalizedMac: String): String {
        return if (normalizedMac.length >= 6) {
            normalizedMac.substring(0, 6)
        } else {
            normalizedMac
        }
    }
}
