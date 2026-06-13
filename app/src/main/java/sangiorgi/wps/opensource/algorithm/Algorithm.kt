package sangiorgi.wps.opensource.algorithm

import android.content.Context
import android.util.Log
import sangiorgi.wps.opensource.algorithm.strategy.AlgorithmException
import sangiorgi.wps.opensource.algorithm.strategy.AlgorithmFactory

/**
 * WPS PIN generation algorithms implementation using Strategy pattern.
 * This class delegates to specific algorithm implementations via the Strategy pattern.
 *
 * Kotlin-idiomatic implementation with:
 * - Sealed class for type-safe algorithm results
 * - Extension functions for cleaner API
 * - Functional programming patterns
 *
 * Design Pattern Benefits:
 * - Open/Closed Principle: New algorithms can be added without modifying this class
 * - Single Responsibility: Each algorithm is in its own class
 * - Strategy Pattern: Algorithms are interchangeable at runtime
 * - Factory Pattern: Algorithm instantiation is centralized
 * - Better testability: Each algorithm can be unit tested independently
 *
 * @param algorithmFactory The factory for creating algorithm instances
 */
class Algorithm private constructor(
    private val algorithmFactory: AlgorithmFactory,
) {
    /**
     * Creates an Algorithm with Android context.
     */
    constructor(context: Context) : this(AlgorithmFactory(context))

    /**
     * Generates a WPS PIN using the specified algorithm.
     * Uses the Strategy pattern to delegate to the appropriate algorithm implementation.
     *
     * @param algorithmType The algorithm to use
     * @param bssid The router's BSSID (MAC address)
     * @param ssid The router's SSID (network name)
     * @return AlgorithmResult containing the PIN and status
     */
    fun generatePin(algorithmType: AlgorithmType, bssid: String, ssid: String?): AlgorithmResult {
        val algorithm = algorithmFactory.getAlgorithm(algorithmType)
            ?: return AlgorithmResult.failure("Unsupported algorithm type")

        // Validate input using the algorithm's validation
        if (!algorithm.validateInput(bssid, ssid)) {
            return AlgorithmResult.failure("Invalid input for algorithm $algorithmType")
        }

        return try {
            // Execute the algorithm strategy
            val pin = algorithm.generatePin(bssid, ssid)
            AlgorithmResult.success(pin, algorithm.algorithmName)
        } catch (e: AlgorithmException) {
            Log.e(TAG, "Algorithm error for $algorithmType", e)
            AlgorithmResult.failure(e.message ?: "Algorithm error")
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error generating PIN with algorithm $algorithmType", e)
            AlgorithmResult.failure("Error: ${e.message}")
        }
    }

    /**
     * Generates suggested PINs for a WiFi network using all auto-suggested algorithms.
     * These are algorithms that don't require serial files.
     *
     * @param bssid The router's BSSID (MAC address)
     * @param ssid The router's SSID (network name)
     * @return List of successful algorithm results with generated PINs
     */
    fun generateSuggestedPins(bssid: String, ssid: String?): List<AlgorithmResult.Success> {
        return AUTO_SUGGESTED_ALGORITHMS
            .map { generatePin(it, bssid, ssid) }
            .filterIsInstance<AlgorithmResult.Success>()
    }

    /**
     * Generates suggested PINs for a WiFi network using all auto-suggested algorithms.
     * Returns only unique PINs (eliminates duplicates), keeping the first occurrence.
     *
     * @param bssid The router's BSSID (MAC address)
     * @param ssid The router's SSID (network name)
     * @return List of unique successful algorithm results
     */
    fun generateUniqueSuggestedPins(bssid: String, ssid: String?): List<AlgorithmResult.Success> {
        return generateSuggestedPins(bssid, ssid)
            .distinctBy { it.pin }
    }

    companion object {
        private const val TAG = "Algorithm"

        /**
         * Creates an Algorithm from an already-built [AlgorithmFactory]. Preferred over the
         * Context constructor when the factory is supplied by dependency injection, which keeps
         * this class testable with a fake factory.
         */
        fun from(algorithmFactory: AlgorithmFactory): Algorithm = Algorithm(algorithmFactory)

        /**
         * Algorithms that are automatically suggested (don't require serial files).
         */
        @JvmField
        val AUTO_SUGGESTED_ALGORITHMS: Array<AlgorithmType> = arrayOf(
            // Bit-based algorithms (PIN = 24-bit)
            AlgorithmType.PIN,
            AlgorithmType.TWENTY_EIGHT_BIT,
            AlgorithmType.THIRTY_TWO_BIT,
            AlgorithmType.THIRTY_SIX_BIT,
            AlgorithmType.FORTY_BIT,
            AlgorithmType.FORTY_FOUR_BIT,
            AlgorithmType.FORTY_EIGHT_BIT,
            // Router-specific algorithms
            AlgorithmType.DLINK,
            AlgorithmType.DLINK_PLUS_ONE,
            AlgorithmType.TRENDNET,
            AlgorithmType.ARRIS,
            AlgorithmType.ASUS,
            AlgorithmType.AIROCON_REALTEK,
            AlgorithmType.ARCADYAN,
            // FTE needs SSID + MAC
            AlgorithmType.FTE,
        )
    }
}
