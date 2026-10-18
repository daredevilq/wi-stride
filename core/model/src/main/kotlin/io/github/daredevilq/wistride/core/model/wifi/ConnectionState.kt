package io.github.daredevilq.wistride.core.model.wifi

import io.github.daredevilq.wistride.core.model.Availability

/**
 * What the device's Wi-Fi is doing right now. The location switch is reported separately
 * in [WifiEnvironment] because it only redacts fields and does not change the connection itself.
 */
sealed interface ConnectionState {
    /** Wi-Fi radio is off and airplane mode is on. */
    data object AirplaneMode : ConnectionState

    /** Wi-Fi radio is off (airplane mode off). */
    data object WifiOff : ConnectionState

    /** Wi-Fi is on (or turning on) but no Wi-Fi network is connected. */
    data object Disconnected : ConnectionState

    data class Connected(
        val connection: WifiConnection,
    ) : ConnectionState
}

/** Device-level switches that explain missing values or suggest an action. */
data class WifiEnvironment(
    val wifiEnabled: Boolean,
    val airplaneModeOn: Boolean,
    val locationEnabled: Boolean,
)

/** Snapshot of the connection emitted by the repository; [ConnectionState] plus [environment]. */
data class WifiSnapshot(
    val state: ConnectionState,
    val environment: WifiEnvironment,
)

/**
 * Live parameters of the connected Wi-Fi network. Fields that are derived purely
 * from others (band, channel number, signal quality, generation) are computed by `domain:wifi`.
 */
data class WifiConnection(
    /** Without quotes; hidden networks and redacted values are [Availability.Unavailable]. */
    val ssid: Availability<String>,
    val bssid: Availability<String>,
    val rssiDbm: Availability<Int>,
    val frequencyMhz: Availability<Int>,
    /** From the scan result of the connected BSSID (not part of `WifiInfo`). */
    val channelWidth: Availability<ChannelWidth>,
    val standard: Availability<WifiStandard>,
    val txLinkSpeedMbps: Availability<Int>,
    val rxLinkSpeedMbps: Availability<Int>,
    val maxTxLinkSpeedMbps: Availability<Int>,
    val maxRxLinkSpeedMbps: Availability<Int>,
    val security: Availability<WifiSecurity>,
    val ip: IpConfiguration,
)

/** Layer-3 configuration from `LinkProperties`. */
data class IpConfiguration(
    val ipv4: Availability<IpAddressWithPrefix>,
    val ipv6: List<IpAddressWithPrefix>,
    val gateway: Availability<String>,
    val dnsServers: Availability<List<String>>,
    val mtu: Availability<Int>,
)

/** Textual address (no leading slash) and prefix length, e.g. `192.168.1.23` / 24. */
data class IpAddressWithPrefix(
    val address: String,
    val prefixLength: Int,
)
