package sangiorgi.wps.opensource.utils

import android.util.Log
import sangiorgi.wps.opensource.domain.models.WpsInfo
import sangiorgi.wps.opensource.domain.models.WpsMethod

/**
 * Pure parser for `iw dev <iface> scan dump` output. Extracted from [IwScanner] so the parsing and
 * WPS config-methods decoding can be unit tested without root or an Android device.
 *
 * WPS Config Methods bit flags:
 *  0x0001 = USB, 0x0002 = Ethernet, 0x0004 = Label, 0x0008 = Display,
 *  0x0010 = External NFC Token, 0x0020 = Integrated NFC Token, 0x0040 = NFC Interface,
 *  0x0080 = Push Button, 0x0100 = Keypad, 0x0280 = Virtual Push Button,
 *  0x0480 = Physical Push Button, 0x2008 = Virtual Display PIN.
 */
internal object IwScanResultParser {

    private const val TAG = "IwScanResultParser"

    private const val PUSH_BUTTON_MASK = 0x0080 or 0x0280 or 0x0480
    private const val PIN_MASK = 0x0004 or 0x0008 or 0x0100 or 0x2008
    private const val NFC_MASK = 0x0010 or 0x0020 or 0x0040

    private const val WPS_STATE_CONFIGURED = 2

    /**
     * Parse WPS information for every BSS in the scan dump, keyed by upper-case BSSID.
     */
    fun parse(lines: List<String>): Map<String, WpsInfo> {
        val result = mutableMapOf<String, WpsInfo>()
        var currentBssid: String? = null
        var wpsState: Int? = null
        var configMethods: Int? = null
        var deviceName: String? = null
        var manufacturer: String? = null
        var modelName: String? = null
        var modelNumber: String? = null

        fun flush() {
            val bssid = currentBssid ?: return
            val wpsInfo = buildWpsInfo(wpsState, configMethods, deviceName, manufacturer, modelName, modelNumber)
                ?: return
            result[bssid.uppercase()] = wpsInfo
        }

        for (line in lines) {
            val trimmed = line.trim()

            if (trimmed.startsWith("BSS ")) {
                flush()
                currentBssid = extractBssid(trimmed)
                wpsState = null
                configMethods = null
                deviceName = null
                manufacturer = null
                modelName = null
                modelNumber = null
                continue
            }

            when {
                trimmed.startsWith("* Wi-Fi Protected Setup State:") -> wpsState = extractNumber(trimmed)
                trimmed.startsWith("* Config methods:") -> configMethods = extractConfigMethods(trimmed)
                trimmed.startsWith("* Device name:") -> deviceName = extractValue(trimmed)
                trimmed.startsWith("* Manufacturer:") -> manufacturer = extractValue(trimmed)
                trimmed.startsWith("* Model:") -> modelName = extractValue(trimmed)
                trimmed.startsWith("* Model Number:") -> modelNumber = extractValue(trimmed)
            }
        }

        flush()
        Log.d(TAG, "Parsed ${result.size} networks with WPS info")
        return result
    }

    private fun extractBssid(line: String): String? {
        // Line format: "BSS aa:bb:cc:dd:ee:ff(on wlan0)" or "BSS aa:bb:cc:dd:ee:ff"
        val regex = Regex("BSS ([0-9a-fA-F:]{17})")
        return regex.find(line)?.groupValues?.get(1)
    }

    private fun extractNumber(line: String): Int? {
        return Regex("(\\d+)").find(line)?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun extractValue(line: String): String? {
        val parts = line.split(":", limit = 2)
        return if (parts.size == 2) parts[1].trim() else null
    }

    private fun extractConfigMethods(line: String): Int {
        // Config methods is a hex value like 0x008c
        val match = Regex("0x([0-9a-fA-F]+)").find(line)
        return match?.groupValues?.get(1)?.toIntOrNull(16) ?: 0
    }

    private fun buildWpsInfo(
        wpsState: Int?,
        configMethods: Int?,
        deviceName: String?,
        manufacturer: String?,
        modelName: String?,
        modelNumber: String?,
    ): WpsInfo? {
        // If no WPS state was found, WPS is not advertised for this BSS.
        if (wpsState == null) return null

        val config = configMethods ?: 0
        val hasPushButton = (config and PUSH_BUTTON_MASK) != 0
        val hasPin = (config and PIN_MASK) != 0
        val hasNfc = (config and NFC_MASK) != 0

        val methods = buildList {
            if (hasPushButton) add(WpsMethod.PUSH_BUTTON)
            if (hasPin) add(WpsMethod.PIN)
            if (hasNfc) add(WpsMethod.NFC)
        }

        // iw doesn't report locked state, so isLocked stays false. wpsState == WPS_STATE_CONFIGURED
        // means the AP is already configured (vs. 1 = not configured); both still expose WPS.
        return WpsInfo(
            isEnabled = true,
            isPbcSupported = hasPushButton,
            isPinSupported = hasPin,
            isLocked = false,
            configMethods = methods,
            deviceName = deviceName,
            manufacturer = manufacturer,
            modelName = modelName,
            modelNumber = modelNumber,
            isFromIw = true,
        )
    }
}
