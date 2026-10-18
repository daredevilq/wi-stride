package io.github.daredevilq.wistride.feature.now

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import io.github.daredevilq.wistride.core.designsystem.theme.WiStrideTheme
import io.github.daredevilq.wistride.core.model.Availability
import io.github.daredevilq.wistride.core.model.UnavailableReason
import io.github.daredevilq.wistride.core.model.permission.PermissionStatus
import io.github.daredevilq.wistride.core.model.wifi.ConnectionState
import io.github.daredevilq.wistride.core.model.wifi.WifiSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NowScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private fun show(
        state: NowUiState,
        actions: NowActions = NowActions(),
    ) {
        rule.setContent { WiStrideTheme { NowScreen(state = state, actions = actions) } }
    }

    private fun loaded(
        snapshot: WifiSnapshot,
        permission: PermissionStatus = PermissionStatus.Granted,
        history: List<Int?> = listOf(-60, -58, -55),
    ) = snapshot.toUiState(permission, history)

    private fun nodeCount(text: String) = rule.onAllNodesWithText(text).fetchSemanticsNodes().size

    private fun assertNodeCount(
        text: String,
        expected: Int,
    ) = assertEquals(text, expected, nodeCount(text))

    private fun unavailable(reason: UnavailableReason) = Availability.Unavailable(reason)

    @Test
    fun loadingShowsProgressText() {
        show(NowUiState.Loading)
        rule.onNodeWithText("Reading Wi-Fi status…").assertExists()
    }

    @Test
    fun connectedShowsValuesChartSummaryAndSections() {
        show(loaded(snapshot()))
        listOf(
            "HomeNet",
            "aa:bb:cc:dd:ee:ff",
            "-55 dBm",
            "90% · Good",
            "5180 MHz",
            "5 GHz",
            "36",
            "80 MHz",
            "Wi-Fi 6 (802.11ax)",
            "866 Mb/s",
            "WPA3-Personal",
            "192.168.1.23/24",
            "fe80::1/64",
            "192.168.1.1",
            "1.1.1.1\n8.8.8.8",
            "1500",
            "Now: -55 dBm · Min: -60 dBm · Max: -55 dBm",
        ).forEach { rule.onNodeWithText(it).assertExists() }
        rule.onNodeWithText("Radio").assertExists()
        rule.onNodeWithText("IP").assertExists()
        assertNodeCount("Unavailable", 0)
    }

    @Test
    fun unavailableFieldsShowReasonAndNeverZero() {
        val connection =
            connection(
                ssid = unavailable(UnavailableReason.NeedsPermission("p")),
                bssid = unavailable(UnavailableReason.NeedsLocationOn),
                txLinkSpeedMbps = unavailable(UnavailableReason.RequiresApi(30)),
                rxLinkSpeedMbps = unavailable(UnavailableReason.RequiresApi(33)),
                channelWidth = unavailable(UnavailableReason.AwaitingData),
                maxTxLinkSpeedMbps = unavailable(UnavailableReason.NotReportedByDevice),
                maxRxLinkSpeedMbps = unavailable(UnavailableReason.NotReportedByDevice),
            )
        show(loaded(snapshot(ConnectionState.Connected(connection))))
        assertTrue(nodeCount("Unavailable") >= 7)
        listOf(
            "Needs the location and nearby devices permission",
            "Turn on location services",
            "Requires Android 11",
            "Requires Android 13",
            "Waiting for a Wi-Fi scan",
            "Not reported by this device or network",
        ).forEach { assertTrue(it, nodeCount(it) >= 1) }
        assertNodeCount("0", 0)
        assertNodeCount("0 Mb/s", 0)
    }

    @Test
    fun unavailableRssiShowsPlaceholderInsteadOfChart() {
        show(loaded(snapshot(ConnectionState.Connected(connection(rssiDbm = Redacted))), history = listOf(null, null)))
        rule.onNodeWithText("No signal readings yet.").assertExists()
        rule.onNodeWithText("-55 dBm").assertDoesNotExist()
    }

    @Test
    fun wifiOffOffersWifiPanel() {
        var opened = 0
        show(loaded(snapshot(ConnectionState.WifiOff)), NowActions(onOpenWifiPanel = { opened++ }))
        rule.onNodeWithText("Wi-Fi is off").assertExists()
        rule.onNodeWithText("Open Wi-Fi settings").performClick()
        assertEquals(1, opened)
        rule.onNodeWithText("Radio").assertDoesNotExist()
    }

    @Test
    fun airplaneModeOffersAirplaneSettings() {
        var opened = 0
        show(loaded(snapshot(ConnectionState.AirplaneMode)), NowActions(onOpenAirplaneSettings = { opened++ }))
        rule.onNodeWithText("Airplane mode is on").assertExists()
        rule.onNode(hasText("switch Wi-Fi back on", substring = true)).assertExists()
        rule.onNodeWithText("Airplane mode settings").performClick()
        assertEquals(1, opened)
    }

    @Test
    fun disconnectedOffersWifiPanel() {
        var opened = 0
        show(loaded(snapshot(ConnectionState.Disconnected)), NowActions(onOpenWifiPanel = { opened++ }))
        rule.onNodeWithText("Not connected to Wi-Fi").assertExists()
        rule.onNodeWithText("Choose a network").performClick()
        assertEquals(1, opened)
    }

    @Test
    fun missingPermissionShowsRationaleCardAndRequests() {
        var requests = 0
        show(
            loaded(snapshot(), permission = PermissionStatus.NotRequested),
            NowActions(onRequestPermission = { requests++ }),
        )
        rule.onNodeWithText("Allow access to Wi-Fi details").assertExists()
        rule.onNodeWithText("Allow").performClick()
        assertEquals(1, requests)
    }

    @Test
    fun permanentlyDeniedOpensAppSettings() {
        var opened = 0
        show(
            loaded(snapshot(), permission = PermissionStatus.PermanentlyDenied),
            NowActions(onOpenAppSettings = { opened++ }),
        )
        rule.onNodeWithText("Open settings").performClick()
        assertEquals(1, opened)
    }

    @Test
    fun locationOffShowsCardOnlyWhenPermissionGranted() {
        var opened = 0
        show(
            loaded(snapshot(environment = DefaultEnvironment.copy(locationEnabled = false))),
            NowActions(onOpenLocationSettings = { opened++ }),
        )
        rule.onNodeWithText("Location services are off").assertExists()
        rule.onNodeWithText("Location settings").performClick()
        assertEquals(1, opened)
    }

    @Test
    fun locationCardHiddenWithoutPermission() {
        show(
            loaded(
                snapshot(environment = DefaultEnvironment.copy(locationEnabled = false)),
                permission = PermissionStatus.Denied,
            ),
        )
        rule.onNodeWithText("Location services are off").assertDoesNotExist()
    }

    @Test
    fun longPressCopiesAvailableValueOnly() {
        val copied = mutableListOf<String>()
        show(loaded(snapshot()), NowActions(onCopy = { copied += it }))
        rule.onNodeWithText("192.168.1.23/24").performScrollTo().performTouchInput { longClick() }
        assertEquals(listOf("192.168.1.23/24"), copied)
    }
}
