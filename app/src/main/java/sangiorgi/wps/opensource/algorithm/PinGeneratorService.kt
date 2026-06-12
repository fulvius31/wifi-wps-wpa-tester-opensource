package sangiorgi.wps.opensource.algorithm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import sangiorgi.wps.opensource.algorithm.strategy.AlgorithmFactory
import sangiorgi.wps.opensource.data.database.PinDatabaseHelper
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service that generates WPS PINs by combining:
 * 1. Static/default PINs from pin.db (based on MAC prefix)
 * 2. Algorithm-generated PINs
 *
 * This provides a comprehensive list of PINs to try for a given network.
 */
@Singleton
class PinGeneratorService @Inject constructor(
    private val algorithmFactory: AlgorithmFactory,
    private val pinDatabaseHelper: PinDatabaseHelper,
) {
    private val algorithm: Algorithm by lazy { Algorithm.from(algorithmFactory) }

    /**
     * Data class representing a PIN with its source.
     */
    data class PinWithSource(
        val pin: String,
        val source: String,
        val isFromDatabase: Boolean = false,
    )

    /**
     * Generate all suggested PINs for a network.
     * Combines database PINs (from MAC prefix lookup) with algorithm-generated PINs.
     *
     * @param bssid The router's BSSID (MAC address)
     * @param ssid The router's SSID (network name)
     * @return List of PINs with their sources, deduplicated
     */
    suspend fun generateAllPins(bssid: String, ssid: String?): List<PinWithSource> = withContext(Dispatchers.IO) {
        val pins = mutableListOf<PinWithSource>()
        val seenPins = mutableSetOf<String>()

        // 1. First, get static PINs from database (these are known defaults for this vendor)
        val databasePins = pinDatabaseHelper.getPinsByMac(bssid)
        for (pin in databasePins) {
            if (pin !in seenPins) {
                seenPins.add(pin)
                pins.add(PinWithSource(pin, "Database (vendor default)", isFromDatabase = true))
            }
        }

        // 2. Then, get algorithm-generated PINs
        val algorithmResults = algorithm.generateUniqueSuggestedPins(bssid, ssid)
        for (result in algorithmResults) {
            if (result.pin !in seenPins) {
                seenPins.add(result.pin)
                pins.add(PinWithSource(result.pin, result.algorithmName, isFromDatabase = false))
            }
        }

        // 3. Add fallback default PIN if not already present
        if (DEFAULT_PIN !in seenPins) {
            pins.add(PinWithSource(DEFAULT_PIN, "Default", isFromDatabase = false))
        }

        pins
    }

    companion object {
        private const val DEFAULT_PIN = "12345670"
    }
}
