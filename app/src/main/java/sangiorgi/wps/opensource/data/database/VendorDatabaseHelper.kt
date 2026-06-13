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
        private const val UNKNOWN_VENDOR = "Unknown"
    }

    @Volatile
    private var database: SQLiteDatabase? = null

    // Set once a re-extraction still failed to produce a readable DB, so we stop re-copying the
    // asset on every lookup (getVendorByMac is called once per network during a scan).
    @Volatile
    private var recoveryFailed = false
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

    /**
     * Force a fresh copy from assets and reopen — recovery path when the on-disk DB is corrupt or
     * was left half-written by a previous run.
     */
    @Synchronized
    private fun reextractAndReopen() {
        Log.w(TAG, "Re-extracting $DATABASE_FILE after a read failure")
        closeQuietly()
        File(databasePath).delete()
        extractDatabaseIfNeeded()
        openDatabase()
    }

    private fun closeQuietly() {
        try {
            database?.close()
        } catch (e: SQLiteException) {
            Log.w(TAG, "Error closing database", e)
        }
        database = null
    }

    private fun extractDatabaseIfNeeded() {
        val dbFile = File(databasePath)
        if (dbFile.exists()) return

        // Write to a temp file and atomically rename, so an interrupted copy never leaves a
        // half-written (corrupt) vendor.db behind that we'd then treat as valid.
        val tmpFile = File("$databasePath.tmp")
        try {
            context.assets.open(DATABASE_FILE).use { input ->
                FileOutputStream(tmpFile).use { output ->
                    input.copyTo(output)
                    output.flush()
                    output.fd.sync()
                }
            }
            if (!tmpFile.renameTo(dbFile)) {
                throw java.io.IOException("Could not rename $tmpFile to $dbFile")
            }
            Log.d(TAG, "Extracted $DATABASE_FILE to: $databasePath")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract $DATABASE_FILE", e)
            tmpFile.delete()
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
        val macPrefix = extractMacPrefix(normalizeMacAddress(macAddress))

        // First attempt; if the DB read fails (e.g. SQLiteDatabaseCorruptException from a
        // malformed/partial vendor.db), re-extract a clean copy from assets and retry once.
        queryVendor(macPrefix) ?: run {
            if (recoveryFailed) {
                return@run UNKNOWN_VENDOR
            }
            reextractAndReopen()
            val retry = queryVendor(macPrefix)
            // If a fresh copy still can't be read, the bundled asset itself is bad — latch so we
            // don't re-extract on every subsequent lookup.
            if (retry == null) {
                recoveryFailed = true
            }
            retry ?: UNKNOWN_VENDOR
        }
    }

    /**
     * Run the vendor lookup. Returns the vendor (or [UNKNOWN_VENDOR] when not found), or null when
     * the query fails — which signals the caller to attempt recovery. Synchronized so a cursor is
     * never read while [reextractAndReopen] closes the database underneath it.
     */
    @Synchronized
    private fun queryVendor(macPrefix: String): String? {
        ensureDatabaseOpen()
        return try {
            database?.let { db ->
                db.rawQuery(
                    "SELECT $COLUMN_VENDOR FROM $TABLE_NAME WHERE $COLUMN_MAC = ? LIMIT 1",
                    arrayOf(macPrefix),
                ).use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) ?: UNKNOWN_VENDOR else UNKNOWN_VENDOR
                }
            }
        } catch (e: SQLiteException) {
            Log.e(TAG, "Vendor query failed for prefix $macPrefix", e)
            null
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
