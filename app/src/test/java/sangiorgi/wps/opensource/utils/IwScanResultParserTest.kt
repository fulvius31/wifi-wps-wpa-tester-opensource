package sangiorgi.wps.opensource.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import sangiorgi.wps.opensource.domain.models.WpsMethod

/** Unit tests for [IwScanResultParser], the pure `iw scan dump` parser. */
class IwScanResultParserTest {

    private fun lines(text: String): List<String> = text.trimIndent().lines()

    @Test
    fun parsesBssidAndWpsFlags() {
        // 0x0188 = Keypad (0x0100) | Push Button (0x0080) | Display (0x0008) -> both PBC and PIN.
        val out = lines(
            """
            BSS aa:bb:cc:dd:ee:ff(on wlan0)
            	freq: 2412
            	WPS:	 * Version: 1.0
            		 * Wi-Fi Protected Setup State: 2 (Configured)
            		 * Manufacturer: Cisco
            		 * Model: RV340
            		 * Model Number: ABC123
            		 * Device name: HomeRouter
            		 * Config methods: 0x0188
            """,
        )

        val result = IwScanResultParser.parse(out)

        assertEquals(1, result.size)
        val info = result["AA:BB:CC:DD:EE:FF"]
        requireNotNull(info) { "BSSID should be present and upper-cased" }
        assertTrue(info.isEnabled)
        assertTrue(info.isFromIw)
        assertFalse("iw never reports locked state", info.isLocked)
        assertTrue(info.isPbcSupported)
        assertTrue(info.isPinSupported)
        assertTrue(info.configMethods.contains(WpsMethod.PUSH_BUTTON))
        assertTrue(info.configMethods.contains(WpsMethod.PIN))
        assertFalse(info.configMethods.contains(WpsMethod.NFC))
        assertEquals("HomeRouter", info.deviceName)
        assertEquals("Cisco", info.manufacturer)
        assertEquals("RV340", info.modelName)
        assertEquals("ABC123", info.modelNumber)
    }

    @Test
    fun pushButtonOnlyHasNoPin() {
        // 0x0080 = Push Button only.
        val out = lines(
            """
            BSS 11:22:33:44:55:66(on wlan0)
            		 * Wi-Fi Protected Setup State: 1 (Unconfigured)
            		 * Config methods: 0x0080
            """,
        )

        val info = IwScanResultParser.parse(out)["11:22:33:44:55:66"]
        requireNotNull(info)
        assertTrue(info.isPbcSupported)
        assertFalse(info.isPinSupported)
    }

    @Test
    fun nfcMethodsAreDetected() {
        // 0x0040 = NFC Interface only.
        val out = lines(
            """
            BSS 01:02:03:04:05:06(on wlan0)
            		 * Wi-Fi Protected Setup State: 2
            		 * Config methods: 0x0040
            """,
        )

        val info = IwScanResultParser.parse(out)["01:02:03:04:05:06"]
        requireNotNull(info)
        assertTrue(info.configMethods.contains(WpsMethod.NFC))
        assertFalse(info.isPbcSupported)
        assertFalse(info.isPinSupported)
    }

    @Test
    fun bssWithoutWpsStateIsExcluded() {
        val out = lines(
            """
            BSS de:ad:be:ef:00:01(on wlan0)
            	freq: 5180
            	signal: -42.00 dBm
            """,
        )

        assertTrue(IwScanResultParser.parse(out).isEmpty())
    }

    @Test
    fun parsesMultipleBssEntriesIndependently() {
        val out = lines(
            """
            BSS aa:aa:aa:aa:aa:aa(on wlan0)
            		 * Wi-Fi Protected Setup State: 2
            		 * Config methods: 0x0188
            BSS bb:bb:bb:bb:bb:bb(on wlan0)
            		 * Wi-Fi Protected Setup State: 1
            		 * Config methods: 0x0080
            BSS cc:cc:cc:cc:cc:cc(on wlan0)
            	freq: 2437
            """,
        )

        val result = IwScanResultParser.parse(out)

        // Only the two BSS entries that advertise WPS are kept; the third (no WPS state) is dropped.
        assertEquals(2, result.size)
        assertTrue(result.getValue("AA:AA:AA:AA:AA:AA").isPinSupported)
        assertFalse(result.getValue("BB:BB:BB:BB:BB:BB").isPinSupported)
        assertNull(result["CC:CC:CC:CC:CC:CC"])
    }

    @Test
    fun emptyOutputYieldsEmptyMap() {
        assertTrue(IwScanResultParser.parse(emptyList()).isEmpty())
    }

    @Test
    fun missingConfigMethodsStillReportsWpsEnabled() {
        val out = lines(
            """
            BSS 0a:0b:0c:0d:0e:0f(on wlan0)
            		 * Wi-Fi Protected Setup State: 2
            """,
        )

        val info = IwScanResultParser.parse(out)["0A:0B:0C:0D:0E:0F"]
        requireNotNull(info)
        assertTrue(info.isEnabled)
        assertFalse(info.isPbcSupported)
        assertFalse(info.isPinSupported)
        assertTrue(info.configMethods.isEmpty())
    }
}
