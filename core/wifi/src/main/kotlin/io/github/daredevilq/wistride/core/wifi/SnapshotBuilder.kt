package io.github.daredevilq.wistride.core.wifi

import android.Manifest
import android.os.Build
import io.github.daredevilq.wistride.core.model.Availability
import io.github.daredevilq.wistride.core.model.UnavailableReason
import io.github.daredevilq.wistride.core.model.available
import io.github.daredevilq.wistride.core.model.unavailable
import io.github.daredevilq.wistride.core.model.wifi.ChannelWidth
import io.github.daredevilq.wistride.core.model.wifi.ConnectionState
import io.github.daredevilq.wistride.core.model.wifi.IpConfiguration
import io.github.daredevilq.wistride.core.model.wifi.WifiConnection
import io.github.daredevilq.wistride.core.model.wifi.WifiEnvironment
import io.github.daredevilq.wistride.core.model.wifi.WifiSnapshot
import io.github.daredevilq.wistride.core.wifi.source.RawLinkInfo
import io.github.daredevilq.wistride.core.wifi.source.RawScanResult
import io.github.daredevilq.wistride.core.wifi.source.WifiNetworkInfo
import io.github.daredevilq.wistride.domain.wifi.channelWidthFromAndroid
import io.github.daredevilq.wistride.domain.wifi.linkSpeedOrNull
import io.github.daredevilq.wistride.domain.wifi.normalizeBssid
import io.github.daredevilq.wistride.domain.wifi.normalizeSsid
import io.github.daredevilq.wistride.domain.wifi.rssiOrNull
import io.github.daredevilq.wistride.domain.wifi.wifiSecurityFromAndroid
import io.github.daredevilq.wistride.domain.wifi.wifiStandardFromAndroid

private const val API_MAX_SPEED_AND_STANDARD = Build.VERSION_CODES.R
private const val API_SECURITY_TYPE = Build.VERSION_CODES.S
private const val LAST_API_SCANS_NEED_LOCATION = Build.VERSION_CODES.S_V2

/** What the platform and the user currently allow; decides why a value is missing. */
internal data class WifiAccess(
    val hasFineLocation: Boolean,
    val canReadScanResults: Boolean,
    val locationEnabled: Boolean,
    val sdkInt: Int,
)

/** Pure mapping of raw platform values to [WifiSnapshot] (a missing value never becomes 0). */
internal fun buildSnapshot(
    wifiEnabled: Boolean,
    airplaneModeOn: Boolean,
    network: WifiNetworkInfo?,
    scanResults: List<RawScanResult>,
    access: WifiAccess,
): WifiSnapshot {
    val state =
        when {
            network != null -> ConnectionState.Connected(buildConnection(network, scanResults, access))
            !wifiEnabled && airplaneModeOn -> ConnectionState.AirplaneMode
            !wifiEnabled -> ConnectionState.WifiOff
            else -> ConnectionState.Disconnected
        }
    return WifiSnapshot(state, WifiEnvironment(wifiEnabled, airplaneModeOn, access.locationEnabled))
}

private fun buildConnection(
    network: WifiNetworkInfo,
    scanResults: List<RawScanResult>,
    access: WifiAccess,
): WifiConnection {
    val ip = network.link?.let(::buildIp) ?: awaitingIp()
    val info = network.wifiInfo ?: return awaitingConnection(ip)
    val bssid = redactableValue(normalizeBssid(info.bssid), access)
    return WifiConnection(
        ssid = redactableValue(normalizeSsid(info.ssid), access),
        bssid = bssid,
        rssiDbm = rssiOrNull(info.rssi).orNotReported(),
        frequencyMhz = info.frequencyMhz.takeIf { it > 0 }.orNotReported(),
        channelWidth = channelWidth(bssid, scanResults, access),
        standard =
            sinceApi(access.sdkInt, API_MAX_SPEED_AND_STANDARD) {
                info.wifiStandard?.let(::wifiStandardFromAndroid).orNotReported()
            },
        txLinkSpeedMbps = linkSpeedOrNull(info.txLinkSpeedMbps).orNotReported(),
        rxLinkSpeedMbps = linkSpeedOrNull(info.rxLinkSpeedMbps).orNotReported(),
        maxTxLinkSpeedMbps =
            sinceApi(access.sdkInt, API_MAX_SPEED_AND_STANDARD) {
                info.maxTxLinkSpeedMbps?.let(::linkSpeedOrNull).orNotReported()
            },
        maxRxLinkSpeedMbps =
            sinceApi(access.sdkInt, API_MAX_SPEED_AND_STANDARD) {
                info.maxRxLinkSpeedMbps?.let(::linkSpeedOrNull).orNotReported()
            },
        security =
            sinceApi(access.sdkInt, API_SECURITY_TYPE) {
                info.currentSecurityType?.let(::wifiSecurityFromAndroid).orNotReported()
            },
        ip = ip,
    )
}

private fun awaitingConnection(ip: IpConfiguration): WifiConnection {
    val awaiting = UnavailableReason.AwaitingData.unavailable()
    return WifiConnection(
        ssid = awaiting,
        bssid = awaiting,
        rssiDbm = awaiting,
        frequencyMhz = awaiting,
        channelWidth = awaiting,
        standard = awaiting,
        txLinkSpeedMbps = awaiting,
        rxLinkSpeedMbps = awaiting,
        maxTxLinkSpeedMbps = awaiting,
        maxRxLinkSpeedMbps = awaiting,
        security = awaiting,
        ip = ip,
    )
}

private fun awaitingIp(): IpConfiguration {
    val awaiting = UnavailableReason.AwaitingData.unavailable()
    return IpConfiguration(
        ipv4 = awaiting,
        ipv6 = emptyList(),
        gateway = awaiting,
        dnsServers = awaiting,
        mtu = awaiting,
    )
}

private fun buildIp(link: RawLinkInfo): IpConfiguration =
    IpConfiguration(
        ipv4 = link.ipv4.firstOrNull().orNotReported(),
        ipv6 = link.ipv6,
        gateway = link.gateway.orNotReported(),
        dnsServers = link.dnsServers.takeIf { it.isNotEmpty() }.orNotReported(),
        mtu = link.mtu.takeIf { it > 0 }.orNotReported(),
    )

/** SSID/BSSID are blanked by the platform without FINE location or with location services off. */
private fun redactableValue(
    value: String?,
    access: WifiAccess,
): Availability<String> =
    when {
        value != null -> {
            value.available()
        }

        !access.hasFineLocation -> {
            UnavailableReason
                .NeedsPermission(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                ).unavailable()
        }

        !access.locationEnabled -> {
            UnavailableReason.NeedsLocationOn.unavailable()
        }

        else -> {
            UnavailableReason.NotReportedByDevice.unavailable()
        }
    }

private fun channelWidth(
    bssid: Availability<String>,
    scanResults: List<RawScanResult>,
    access: WifiAccess,
): Availability<ChannelWidth> {
    val connectedBssid =
        when (bssid) {
            is Availability.Unavailable -> return bssid
            is Availability.Available -> bssid.value
        }
    val scanPermission =
        if (access.sdkInt >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.NEARBY_WIFI_DEVICES
        } else {
            Manifest.permission.ACCESS_FINE_LOCATION
        }
    val match = scanResults.firstOrNull { it.bssid.equals(connectedBssid, ignoreCase = true) }
    return when {
        !access.canReadScanResults -> {
            UnavailableReason.NeedsPermission(scanPermission).unavailable()
        }

        access.sdkInt <= LAST_API_SCANS_NEED_LOCATION && !access.locationEnabled -> {
            UnavailableReason.NeedsLocationOn.unavailable()
        }

        match == null -> {
            UnavailableReason.AwaitingData.unavailable()
        }

        else -> {
            channelWidthFromAndroid(match.channelWidth).orNotReported()
        }
    }
}

private inline fun <T> sinceApi(
    sdkInt: Int,
    requiredApi: Int,
    value: () -> Availability<T>,
): Availability<T> = if (sdkInt < requiredApi) UnavailableReason.RequiresApi(requiredApi).unavailable() else value()

private fun <T> T?.orNotReported(): Availability<T> =
    this?.available() ?: UnavailableReason.NotReportedByDevice.unavailable()
