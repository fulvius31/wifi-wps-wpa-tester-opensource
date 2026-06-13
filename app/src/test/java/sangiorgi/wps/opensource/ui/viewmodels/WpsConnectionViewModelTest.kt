package sangiorgi.wps.opensource.ui.viewmodels

import android.net.wifi.WifiManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import sangiorgi.wps.lib.WpsConnectionManager
import sangiorgi.wps.lib.models.NetworkToTest
import sangiorgi.wps.opensource.domain.models.WifiNetwork
import sangiorgi.wps.opensource.ui.screens.ConnectionMethod
import sangiorgi.wps.opensource.ui.screens.ConnectionStatus

@OptIn(ExperimentalCoroutinesApi::class)
class WpsConnectionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var wpsManager: WpsConnectionManager
    private lateinit var wifiManager: WifiManager
    private lateinit var viewModel: WpsConnectionViewModel

    private val network = WifiNetwork(
        bssid = "AA:BB:CC:DD:EE:FF",
        ssid = "TestNet",
        signalLevel = -50,
        frequency = 2412,
        capabilities = "[WPS][WPA2-PSK-CCMP]",
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        wpsManager = mock()
        wifiManager = mock()
        // Default: WiFi off, so connection attempts proceed past the pre-check.
        whenever(wifiManager.isWifiEnabled).thenReturn(false)
        viewModel = WpsConnectionViewModel(wpsManager, wifiManager, CoroutineScope(testDispatcher))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun updateMessage_extractsLabelledPin() {
        viewModel.updateMessage("Testing PIN: 12345678")
        assertEquals("12345678", viewModel.connectionState.value.currentPin)
    }

    @Test
    fun updateMessage_prefersLabelledPinOverUnrelatedNumber() {
        viewModel.updateMessage("attempt 99999999 — trying PIN 12345670")
        assertEquals("12345670", viewModel.connectionState.value.currentPin)
    }

    @Test
    fun updateMessage_fallsBackToBareEightDigitToken() {
        viewModel.updateMessage("recovered key 87654321 from handshake")
        assertEquals("87654321", viewModel.connectionState.value.currentPin)
    }

    @Test
    fun bruteForce_usesRealWpsSearchSpaceNotTenToTheEight() {
        viewModel.startConnection(network, ConnectionMethod.BRUTE_FORCE)
        assertEquals(11_000, viewModel.connectionState.value.totalPins)
        assertEquals(ConnectionStatus.CONNECTING, viewModel.connectionState.value.status)
    }

    @Test
    fun standardWithPins_setsTotalToPinCount() {
        viewModel.startConnection(
            network,
            ConnectionMethod.STANDARD_WITH_PINS(listOf("12345670", "00000000", "11111111")),
        )
        assertEquals(3, viewModel.connectionState.value.totalPins)
    }

    @Test
    fun cancelConnection_marksCancelledNotFailed() {
        viewModel.startConnection(network, ConnectionMethod.BRUTE_FORCE)
        viewModel.cancelConnection()
        assertEquals(ConnectionStatus.CANCELLED, viewModel.connectionState.value.status)
    }

    @Test
    fun error_marksFailedWithMessage() {
        viewModel.error("WPS is locked", 0)
        val state = viewModel.connectionState.value
        assertEquals(ConnectionStatus.FAILED, state.status)
        assertEquals("WPS is locked", state.errorMessage)
    }

    @Test
    fun success_reportsPinAndPassword() {
        viewModel.updateMessage("Testing PIN 12345670")
        val networkToTest = NetworkToTest().apply { password = "s3cr3tpass" }

        viewModel.success(networkToTest, isRoot = true)

        val state = viewModel.connectionState.value
        assertEquals(ConnectionStatus.SUCCESS, state.status)
        assertEquals("12345670", state.successPin)
        assertEquals("s3cr3tpass", state.password)
    }

    @Test
    fun updateCount_accumulatesProgress() {
        viewModel.updateCount(5)
        viewModel.updateCount(3)
        assertEquals(8, viewModel.connectionState.value.currentPinIndex)
    }

    @Test
    fun startConnection_blocksWhenWifiEnabled() {
        whenever(wifiManager.isWifiEnabled).thenReturn(true)

        viewModel.startConnection(network, ConnectionMethod.BRUTE_FORCE)

        assertEquals(ConnectionStatus.WIFI_ENABLED, viewModel.connectionState.value.status)
        // The native attempt must not start while WiFi holds wlan0.
        verifyNoInteractions(wpsManager)
    }
}
