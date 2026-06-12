package sangiorgi.wps.opensource.algorithm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import sangiorgi.wps.opensource.algorithm.strategy.AlgorithmFactory

/**
 * Invariant tests for the WPS PIN algorithms. Rather than pin down vendor-specific reference
 * vectors, these assert the properties every generated PIN must hold: it is an 8-digit string,
 * its last digit is a valid WPS checksum of the first seven, and generation is deterministic.
 */
class AlgorithmInvariantsTest {

    private val factory = AlgorithmFactory("build/tmp/algo-test/")
    private val algorithm = Algorithm.from(factory)

    private val sampleBssid = "00:11:22:33:44:55"
    private val sampleSsid = "TestNetwork"

    private fun isValidWpsPin(pin: String): Boolean {
        if (!Regex("\\d{8}").matches(pin)) return false
        val firstSeven = pin.substring(0, 7).toInt()
        val checksum = pin.substring(7).toInt()
        return ChecksumCalculator.calculatePreMultiplied(firstSeven) == checksum
    }

    @Test
    fun suggestedPinsAreWellFormedAndChecksumValid() {
        val results = algorithm.generateUniqueSuggestedPins(sampleBssid, sampleSsid)

        assertTrue("Expected at least one suggested PIN for a normal MAC", results.isNotEmpty())
        results.forEach { result ->
            assertTrue(
                "PIN '${result.pin}' from ${result.algorithmName} must be a valid 8-digit WPS PIN",
                isValidWpsPin(result.pin),
            )
        }
    }

    @Test
    fun suggestedPinsAreUnique() {
        val pins = algorithm.generateUniqueSuggestedPins(sampleBssid, sampleSsid).map { it.pin }
        assertEquals("generateUniqueSuggestedPins must not return duplicates", pins.size, pins.toSet().size)
    }

    @Test
    fun generationIsDeterministic() {
        val first = algorithm.generateUniqueSuggestedPins(sampleBssid, sampleSsid).map { it.pin }
        val second = algorithm.generateUniqueSuggestedPins(sampleBssid, sampleSsid).map { it.pin }
        assertEquals(first, second)
    }

    @Test
    fun fileFreeAlgorithmsProduceValidChecksums() {
        // Algorithms that derive the PIN purely from the MAC (no serial/session file needed).
        val fileFreeTypes = listOf(
            AlgorithmType.PIN,
            AlgorithmType.TWENTY_EIGHT_BIT,
            AlgorithmType.THIRTY_TWO_BIT,
            AlgorithmType.THIRTY_SIX_BIT,
            AlgorithmType.FORTY_BIT,
            AlgorithmType.FORTY_FOUR_BIT,
            AlgorithmType.FORTY_EIGHT_BIT,
            AlgorithmType.DLINK,
            AlgorithmType.DLINK_PLUS_ONE,
            AlgorithmType.TRENDNET,
            AlgorithmType.ARRIS,
            AlgorithmType.ASUS,
            AlgorithmType.AIROCON_REALTEK,
            AlgorithmType.ARCADYAN,
        )

        fileFreeTypes.forEach { type ->
            val result = algorithm.generatePin(type, sampleBssid, sampleSsid)
            assertTrue(
                "$type should generate a PIN for a normal MAC, got: $result",
                result is AlgorithmResult.Success,
            )
            val pin = (result as AlgorithmResult.Success).pin
            assertTrue("$type produced an invalid WPS PIN: $pin", isValidWpsPin(pin))
        }
    }
}
