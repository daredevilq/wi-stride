package io.github.daredevilq.wistride.feature.now

import io.github.daredevilq.wistride.core.model.Availability
import io.github.daredevilq.wistride.core.model.UnavailableReason
import io.github.daredevilq.wistride.core.model.available
import io.github.daredevilq.wistride.core.model.wifi.ChannelWidth
import io.github.daredevilq.wistride.core.model.wifi.ConnectionState
import io.github.daredevilq.wistride.core.model.wifi.IpAddressWithPrefix
import io.github.daredevilq.wistride.core.model.wifi.IpConfiguration
import io.github.daredevilq.wistride.core.model.wifi.WifiConnection
import io.github.daredevilq.wistride.core.model.wifi.WifiEnvironment
import io.github.daredevilq.wistride.core.model.wifi.WifiSecurity
import io.github.daredevilq.wistride.core.model.wifi.WifiSnapshot
import io.github.daredevilq.wistride.core.model.wifi.WifiStandard
import io.github.daredevilq.wistride.core.wifi.WifiConnectionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart

val DefaultEnvironment = WifiEnvironment(wifiEnabled = true, airplaneModeOn = false, locationEnabled = true)

fun fullIp() =
    IpConfiguration(
        ipv4 = IpAddressWithPrefix("192.168.1.23", 24).available(),
        ipv6 = listOf(IpAddressWithPrefix("fe80::1", 64)),
        gateway = "192.168.1.1".available(),
        dnsServers = listOf("1.1.1.1", "8.8.8.8").available(),
        mtu = 1500.available(),
    )

@Suppress("LongParameterList") // Test builder: every field is overridable, all have defaults.
fun connection(
    ssid: Availability<String> = "HomeNet".available(),
    bssid: Availability<String> = "aa:bb:cc:dd:ee:ff".available(),
    rssiDbm: Availability<Int> = (-55).available(),
    frequencyMhz: Availability<Int> = 5180.available(),
    channelWidth: Availability<ChannelWidth> = ChannelWidth.Mhz80.available(),
    standard: Availability<WifiStandard> = WifiStandard.Ieee80211ax.available(),
    txLinkSpeedMbps: Availability<Int> = 866.available(),
    rxLinkSpeedMbps: Availability<Int> = 780.available(),
    maxTxLinkSpeedMbps: Availability<Int> = 1200.available(),
    maxRxLinkSpeedMbps: Availability<Int> = 1200.available(),
    security: Availability<WifiSecurity> = WifiSecurity.Sae.available(),
    ip: IpConfiguration = fullIp(),
) = WifiConnection(
    ssid = ssid,
    bssid = bssid,
    rssiDbm = rssiDbm,
    frequencyMhz = frequencyMhz,
    channelWidth = channelWidth,
    standard = standard,
    txLinkSpeedMbps = txLinkSpeedMbps,
    rxLinkSpeedMbps = rxLinkSpeedMbps,
    maxTxLinkSpeedMbps = maxTxLinkSpeedMbps,
    maxRxLinkSpeedMbps = maxRxLinkSpeedMbps,
    security = security,
    ip = ip,
)

fun snapshot(
    state: ConnectionState = ConnectionState.Connected(connection()),
    environment: WifiEnvironment = DefaultEnvironment,
) = WifiSnapshot(state, environment)

fun connectedSnapshot(
    rssi: Int,
    environment: WifiEnvironment = DefaultEnvironment,
) = snapshot(ConnectionState.Connected(connection(rssiDbm = rssi.available())), environment)

val Redacted = Availability.Unavailable(UnavailableReason.NeedsPermission("android.permission.ACCESS_FINE_LOCATION"))

/** Repository whose [snapshots] counts how many collectors are currently subscribed. */
class FakeWifiConnectionRepository(
    initial: WifiSnapshot = snapshot(),
) : WifiConnectionRepository {
    val source = MutableStateFlow(initial)
    var subscriptions = 0
        private set
    var activeCollectors = 0
        private set

    override val snapshots: Flow<WifiSnapshot> =
        source
            .onStart {
                subscriptions++
                activeCollectors++
            }.onCompletion { activeCollectors-- }
}
