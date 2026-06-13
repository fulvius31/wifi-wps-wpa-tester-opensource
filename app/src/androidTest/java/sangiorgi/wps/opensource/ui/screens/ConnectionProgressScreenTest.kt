package sangiorgi.wps.opensource.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import sangiorgi.wps.opensource.domain.models.WifiNetwork
import sangiorgi.wps.opensource.ui.theme.WIFIWPSWPATESTEROPENSOURCETheme

/**
 * UI tests for [ConnectionProgressScreen]'s per-status rendering. Runs on device/emulator
 * (connectedOpenDebugAndroidTest).
 */
class ConnectionProgressScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val network = WifiNetwork(
        bssid = "AA:BB:CC:DD:EE:FF",
        ssid = "TestNet",
        signalLevel = -50,
        frequency = 2412,
        capabilities = "[WPS][WPA2-PSK-CCMP]",
    )

    private fun setScreen(state: ConnectionState) {
        composeRule.setContent {
            WIFIWPSWPATESTEROPENSOURCETheme {
                ConnectionProgressScreen(
                    network = network,
                    connectionMethod = ConnectionMethod.BRUTE_FORCE,
                    connectionState = state,
                    onCancel = {},
                    onClose = {},
                    onRetry = {},
                    onDone = {},
                )
            }
        }
    }

    @Test
    fun wifiEnabledState_showsDisablePromptAndActions() {
        setScreen(ConnectionState(status = ConnectionStatus.WIFI_ENABLED))

        composeRule.onNodeWithText("WiFi is enabled", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Open WiFi Settings").assertIsDisplayed()
        composeRule.onNodeWithText("Retry").assertIsDisplayed()
    }

    @Test
    fun connectingState_showsStop() {
        setScreen(ConnectionState(status = ConnectionStatus.CONNECTING, currentPin = "12345670", totalPins = 11000))

        composeRule.onNodeWithText("Stop").assertIsDisplayed()
    }

    @Test
    fun successState_showsPinAndCopyShareActions() {
        setScreen(
            ConnectionState(
                status = ConnectionStatus.SUCCESS,
                successPin = "12345670",
                password = "s3cr3tpass",
            ),
        )

        composeRule.onNodeWithText("PIN Found!").assertIsDisplayed()
        composeRule.onNodeWithText("12345670").assertIsDisplayed()
        composeRule.onNodeWithText("Copy").assertIsDisplayed()
        composeRule.onNodeWithText("Share").assertIsDisplayed()
    }

    @Test
    fun cancelledState_showsCloseAndRetry() {
        setScreen(ConnectionState(status = ConnectionStatus.CANCELLED))

        composeRule.onNodeWithText("Close").assertIsDisplayed()
        composeRule.onNodeWithText("Retry").assertIsDisplayed()
    }
}
