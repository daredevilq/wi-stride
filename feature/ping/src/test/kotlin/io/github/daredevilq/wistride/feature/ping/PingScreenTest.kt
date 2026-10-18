package io.github.daredevilq.wistride.feature.ping

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import io.github.daredevilq.wistride.core.designsystem.theme.WiStrideTheme
import io.github.daredevilq.wistride.core.model.permission.PermissionStatus
import io.github.daredevilq.wistride.core.model.ping.LatencyUnavailableReason
import io.github.daredevilq.wistride.core.model.ping.PingMethod
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class PingScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private fun show(
        state: PingUiState,
        actions: PingActions = PingActions(),
    ) {
        rule.setContent { WiStrideTheme { PingScreen(state = state, actions = actions) } }
    }

    private fun content(
        vararg targets: TargetUi,
        selectedId: String? = null,
        permission: PermissionStatus = PermissionStatus.Granted,
    ) = PingUiState.Content(targets.toList(), intervalMs = 1_000L, selectedId = selectedId, permission = permission)

    private fun measured(id: String = "gateway") =
        targetUi(
            id = id,
            host = if (id == "gateway") null else id,
            method = PingMethod.Icmp,
            address = "192.168.1.1",
            probes = listOf(10.0, null, 20.0),
            dnsMs = 12.0.takeIf { id != "gateway" },
        )

    private fun count(text: String) = rule.onAllNodesWithText(text).fetchSemanticsNodes().size

    @Test
    fun loadingShowsProgressText() {
        show(PingUiState.Loading)
        rule.onNodeWithText("Preparing measurements…").assertExists()
    }

    @Test
    fun listShowsNameChipsAndStatistics() {
        show(content(measured("google.com")))
        rule.onNodeWithText("google.com").assertExists()
        rule.onNodeWithText("ICMP").assertExists()
        rule.onNodeWithText("DNS 12 ms").assertExists()
        // The clickable card merges its texts: current 20 ms, min 10, avg 15, max 20, jitter 10, loss 33.3 %.
        rule.onNodeWithText("20 ms").assertExists()
        rule.onNodeWithText("10 ms").assertExists()
        rule.onNodeWithText("15 ms").assertExists()
        rule.onNodeWithText("33.3%").assertExists()
        rule.onNodeWithText("Jitter").assertExists()
    }

    @Test
    fun gatewayUsesLocalizedName() {
        show(content(measured("gateway")))
        rule.onNodeWithText("Gateway").assertExists()
        assertEquals(0, count("DNS 12 ms"))
    }

    @Test
    fun tcpChipShowsPort() {
        show(content(targetUi(host = "x.org", method = PingMethod.TcpConnect(8080), probes = listOf(5.0))))
        rule.onNodeWithText("TCP:8080").assertExists()
        rule.onNodeWithText("5.0 ms").assertExists()
    }

    @Test
    fun noDataYetShowsMeasuringNeverZero() {
        show(content(targetUi(host = "x.org")))
        rule.onNodeWithText("Measuring…").assertExists()
        assertEquals(0, count("0 ms"))
    }

    @Test
    fun lostLastProbeShowsNoReply() {
        show(content(targetUi(host = "x.org", probes = listOf(null, null))))
        rule.onNodeWithText("No reply").assertExists()
        assertEquals(0, count("0 ms"))
    }

    @Test
    fun sparklineHasTextSummary() {
        show(content(measured("google.com")))
        rule.onNodeWithContentDescription("Last probes: 3 · min 10 ms · max 20 ms · lost 1").assertExists()
    }

    @Test
    fun tappingACardSelectsIt() {
        var selected: String? = null
        show(content(measured("google.com")), PingActions(onSelect = { selected = it }))
        rule.onNodeWithText("google.com").performClick()
        assertEquals("google.com", selected)
    }

    @Test
    fun emptyOffersRestoringDefaults() {
        var restored = false
        show(PingUiState.Empty(1_000L), PingActions(onRestoreDefaults = { restored = true }))
        rule.onNodeWithText("No targets").assertExists()
        // The same label is also in the overflow menu, which is closed here.
        rule.onNodeWithText("Restore default targets").performClick()
        assertEquals(true, restored)
    }

    @Test
    fun noWifiIsAScreenLevelStateWithWifiPanelAction() {
        var opened = false
        show(PingUiState.NoWifi(1_000L), PingActions(onOpenWifiPanel = { opened = true }))
        rule.onNodeWithText("Not connected to Wi-Fi").assertExists()
        rule.onNodeWithText("Choose a network").performClick()
        assertEquals(true, opened)
    }

    @Test
    fun unavailableReasonsAreShownInsteadOfNumbers() {
        show(
            content(
                targetUi(id = "gateway", unavailable = LatencyUnavailableReason.NoGateway),
                targetUi(id = "a", host = "a.org", unavailable = LatencyUnavailableReason.UnknownHost),
                targetUi(id = "b", host = "b.org", unavailable = LatencyUnavailableReason.NoWifi),
            ),
        )
        rule.onNodeWithText("This network has no gateway to measure.").assertExists()
        rule.onNodeWithText("The host name could not be resolved on this network.").assertExists()
        rule.onNodeWithText("Not connected to Wi-Fi.").assertExists()
        assertEquals(3, count("Unavailable"))
        assertEquals(0, count("0 ms"))
    }

    @Test
    fun needsPermissionCardRequestsPermission() {
        var requested = false
        show(
            content(
                targetUi(unavailable = LatencyUnavailableReason.NeedsPermission("perm")),
                permission = PermissionStatus.NotRequested,
            ),
            PingActions(onRequestPermission = { requested = true }),
        )
        rule.onNodeWithText("Reaching devices on your local network needs permission.").assertExists()
        rule.onNodeWithText("Allow access to local network devices").assertExists()
        rule.onNodeWithText("Allow").performClick()
        assertEquals(true, requested)
    }

    @Test
    fun permanentlyDeniedPermissionOpensAppSettings() {
        var opened = false
        show(
            content(
                targetUi(unavailable = LatencyUnavailableReason.NeedsPermission("perm")),
                permission = PermissionStatus.PermanentlyDenied,
            ),
            PingActions(onOpenAppSettings = { opened = true }),
        )
        rule.onNodeWithText("Open settings").performClick()
        assertEquals(true, opened)
    }

    @Test
    fun addDialogValidatesTheHostLive() {
        show(content(measured()))
        rule.onNodeWithText("Add target").performClick()
        rule.onNodeWithText("Add").assertIsNotEnabled()
        assertEquals(0, count("Enter a host name or an IP address."))

        rule.onAllNodes(hasSetTextAction())[0].performTextInput("bad host")
        rule.onNodeWithText("This is not a valid host name or IP address.").assertExists()
        rule.onNodeWithText("Add").assertIsNotEnabled()
    }

    @Test
    fun addDialogShowsPortErrorAndConfirms() {
        var added: Pair<String, Int?>? = null
        show(content(measured()), PingActions(onAddTarget = { host, port -> added = host to port }))
        rule.onNodeWithText("Add target").performClick()
        rule.onNodeWithText("With a port, the time to open a TCP connection is measured").assertExists()

        val fields = rule.onAllNodes(hasSetTextAction())
        fields[0].performTextInput("Example.org")
        fields[1].performTextInput("99999")
        rule.onNodeWithText("Enter a port from 1 to 65535.").assertExists()
        rule.onNodeWithText("Add").assertIsNotEnabled()

        fields[1].performTextReplacement("443")
        rule.onNodeWithText("Add").assertIsEnabled().performClick()
        assertEquals("example.org" to 443, added)
        assertEquals(0, count("Cancel"))
    }

    @Test
    fun emptyHostErrorAppearsOnlyAfterEditing() {
        show(content(measured()))
        rule.onNodeWithText("Add target").performClick()
        val host = rule.onAllNodes(hasSetTextAction())[0]
        host.performTextInput("a")
        host.performTextReplacement("")
        rule.onNodeWithText("Enter a host name or an IP address.").assertExists()
    }

    @Test
    fun intervalMenuListsAllowedIntervals() {
        var interval = 0L
        show(content(measured()), PingActions(onSetInterval = { interval = it }))
        rule.onNodeWithText("Interval: 1 s").performClick()
        rule.onNodeWithText("0.5 s").assertExists()
        rule.onNodeWithText("1 s (current)").assertExists()
        rule.onNodeWithText("5 s").assertExists()
        rule.onNodeWithText("2 s").performClick()
        assertEquals(2_000L, interval)
    }

    @Test
    fun overflowMenuRestoresDefaults() {
        var restored = false
        show(content(measured()), PingActions(onRestoreDefaults = { restored = true }))
        rule.onNodeWithContentDescription("More options").performClick()
        rule.onNodeWithText("Restore default targets").performClick()
        assertEquals(true, restored)
    }

    @Test
    fun compactDetailReplacesListAndOffersBackAndDelete() {
        var back = false
        var removed: String? = null
        show(
            content(measured("google.com"), selectedId = "google.com"),
            PingActions(onBack = { back = true }, onRemoveTarget = { removed = it }),
        )
        rule.onNodeWithText("Latency, last 120 probes").assertExists()
        rule.onNodeWithText("Last probes: 3 · min 10 ms · max 20 ms · lost 1").assertExists()
        rule.onNodeWithText("Statistics").assertExists()
        rule.onNodeWithText("Resolved address").assertExists()
        rule.onNodeWithText("192.168.1.1").assertExists()
        rule.onNodeWithText("ICMP echo (system ping)").assertExists()
        rule.onNodeWithText("DNS lookup time").assertExists()
        rule.onNodeWithText("2 of 3").assertExists()
        rule.onNodeWithText("Back to the list").performClick()
        assertEquals(true, back)
        rule.onNodeWithText("Delete target").performScrollTo().performClick()
        assertEquals("google.com", removed)
    }

    @Test
    fun detailWithoutAnswersShowsTextInsteadOfChart() {
        show(content(targetUi(host = "x.org", probes = listOf(null, null)), selectedId = "gateway"))
        rule.onNodeWithText("No answered probes yet.").assertExists()
        rule.onNodeWithText("Packet loss").assertExists()
        rule.onNodeWithText("100%").assertExists()
    }

    @Test
    fun detailOfUnavailableTargetShowsReason() {
        show(
            content(
                targetUi(unavailable = LatencyUnavailableReason.NoGateway),
                selectedId = "gateway",
            ),
        )
        rule.onNodeWithText("This network has no gateway to measure.").assertExists()
        assertEquals(0, count("Statistics"))
    }

    @Test
    @Config(qualifiers = "w900dp-h900dp")
    fun wideLayoutShowsListAndDetailSideBySide() {
        show(content(measured("google.com"), selectedId = "google.com"))
        rule.onNodeWithText("Statistics").assertExists()
        // Name in the list card and in the detail header.
        assertEquals(2, count("google.com"))
        assertEquals(0, count("Back to the list"))
    }

    @Test
    @Config(qualifiers = "w900dp-h900dp")
    fun wideLayoutWithoutSelectionShowsPlaceholder() {
        show(content(measured("google.com")))
        rule.onNodeWithText("Select a target to see its chart and details.").assertExists()
        rule.onNodeWithText("google.com").assertExists()
    }
}
