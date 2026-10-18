package io.github.daredevilq.wistride.core.wifi.source

import io.github.daredevilq.wistride.core.model.wifi.IpAddressWithPrefix
import kotlinx.coroutines.flow.Flow

/*
 * Thin wrappers around Android callbacks/broadcasts. Each flow is a cold `callbackFlow` that emits
 * the current value first and unregisters in `awaitClose`. They emit Android-free raw values so the
 * mapping in DefaultWifiConnectionRepository is unit-testable with fakes.
 */

/** Device switches, from WIFI_STATE_CHANGED / AIRPLANE_MODE / location PROVIDERS_CHANGED broadcasts. */
interface WifiStateSource {
    /** True only in `WIFI_STATE_ENABLED`. */
    val wifiEnabled: Flow<Boolean>
    val airplaneModeOn: Flow<Boolean>
    val locationEnabled: Flow<Boolean>
}

/** The connected Wi-Fi network (NetworkCallback for TRANSPORT_WIFI), or null when there is none. */
interface ConnectivitySource {
    val wifiNetwork: Flow<WifiNetworkInfo?>
}

/** Scan results cached by the platform (ours or system-triggered). */
interface ScanResultsSource {
    /** Current cached results first, then after every SCAN_RESULTS_AVAILABLE broadcast. */
    val scanResults: Flow<List<RawScanResult>>

    /** Asks the platform for a scan; false when refused or throttled (≈4 per 2 min in foreground). */
    fun requestScan(): Boolean
}

/** Which of the [WifiPermissions] are currently granted; checked on every snapshot. */
interface WifiPermissionChecker {
    fun hasFineLocation(): Boolean

    /** Permission gating scan results: FINE location up to API 32, NEARBY_WIFI_DEVICES on 33+. */
    fun canReadScanResults(): Boolean
}

data class WifiNetworkInfo(
    /** Null until the platform delivered a `WifiInfo` for the network. */
    val wifiInfo: RawWifiInfo?,
    /** Null until `onLinkPropertiesChanged` arrived. */
    val link: RawLinkInfo?,
)

/**
 * Values copied from `WifiInfo` without interpretation. Nullable fields are null when the API level
 * lacks the getter; sentinel values (INVALID_RSSI, LINK_SPEED_UNKNOWN, `<unknown ssid>`) are kept.
 */
data class RawWifiInfo(
    val ssid: String?,
    val bssid: String?,
    val rssi: Int,
    val frequencyMhz: Int,
    val txLinkSpeedMbps: Int,
    val rxLinkSpeedMbps: Int,
    /** API 30+. */
    val maxTxLinkSpeedMbps: Int?,
    /** API 30+. */
    val maxRxLinkSpeedMbps: Int?,
    /** `ScanResult.WIFI_STANDARD_*`, API 30+. */
    val wifiStandard: Int?,
    /** `WifiInfo.SECURITY_TYPE_*`, API 31+. */
    val currentSecurityType: Int?,
)

/** Values extracted from `LinkProperties`. */
data class RawLinkInfo(
    val ipv4: List<IpAddressWithPrefix>,
    val ipv6: List<IpAddressWithPrefix>,
    /** Next hop of the default route, if any. */
    val gateway: String?,
    val dnsServers: List<String>,
    /** 0 when the network did not report one. */
    val mtu: Int,
)

/** Values copied from `ScanResult` that M1 needs (extended in M3). */
data class RawScanResult(
    val bssid: String,
    val ssid: String?,
    val rssi: Int,
    val frequencyMhz: Int,
    /** `ScanResult.CHANNEL_WIDTH_*`. */
    val channelWidth: Int,
    val centerFreq0Mhz: Int,
    val centerFreq1Mhz: Int,
    /** `SystemClock.elapsedRealtime` microseconds of the last time the AP was seen. */
    val timestampMicros: Long,
)
