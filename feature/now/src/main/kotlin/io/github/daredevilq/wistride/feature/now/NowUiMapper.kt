package io.github.daredevilq.wistride.feature.now

import io.github.daredevilq.wistride.core.model.Availability
import io.github.daredevilq.wistride.core.model.UnavailableReason
import io.github.daredevilq.wistride.core.model.map
import io.github.daredevilq.wistride.core.model.permission.PermissionStatus
import io.github.daredevilq.wistride.core.model.valueOrNull
import io.github.daredevilq.wistride.core.model.wifi.ConnectionState
import io.github.daredevilq.wistride.core.model.wifi.WifiChannel
import io.github.daredevilq.wistride.core.model.wifi.WifiConnection
import io.github.daredevilq.wistride.core.model.wifi.WifiSnapshot
import io.github.daredevilq.wistride.domain.wifi.WifiChannels
import io.github.daredevilq.wistride.domain.wifi.signalQuality
import io.github.daredevilq.wistride.domain.wifi.wifiGeneration

internal fun WifiSnapshot.toUiState(
    permission: PermissionStatus,
    rssiHistory: List<Int?>,
): NowUiState.Loaded =
    NowUiState.Loaded(
        link = state.toLinkStatus(),
        permission = permission,
        locationServicesOff = permission == PermissionStatus.Granted && !environment.locationEnabled,
        rssiHistory = rssiHistory,
    )

/** RSSI to record in the history for this snapshot; null when not connected or RSSI is unavailable. */
internal fun WifiSnapshot?.rssiSample(): Int? =
    ((this?.state as? ConnectionState.Connected)?.connection?.rssiDbm)?.valueOrNull()

private fun ConnectionState.toLinkStatus(): LinkStatus =
    when (this) {
        ConnectionState.AirplaneMode -> LinkStatus.AirplaneMode
        ConnectionState.WifiOff -> LinkStatus.WifiOff
        ConnectionState.Disconnected -> LinkStatus.Disconnected
        is ConnectionState.Connected -> LinkStatus.Connected(connection.toDetails())
    }

private fun WifiConnection.toDetails(): ConnectionDetails {
    // An available frequency that is off the channel grid yields no channel; report it as such, not as 0.
    val channel: Availability<WifiChannel> = frequencyMhz.map { WifiChannels.fromFrequency(it) }.orNotReported()
    val band = channel.map { it.band }
    return ConnectionDetails(
        ssid = ssid,
        bssid = bssid,
        rssiDbm = rssiDbm,
        quality = rssiDbm.valueOrNull()?.let(::signalQuality),
        frequencyMhz = frequencyMhz,
        band = band,
        channel = channel.map { it.number },
        channelWidth = channelWidth,
        generation = standard.map { GenerationInfo(wifiGeneration(it, band.valueOrNull()), it) },
        txLinkSpeedMbps = txLinkSpeedMbps,
        rxLinkSpeedMbps = rxLinkSpeedMbps,
        maxTxLinkSpeedMbps = maxTxLinkSpeedMbps,
        maxRxLinkSpeedMbps = maxRxLinkSpeedMbps,
        security = security,
        ip = ip,
    )
}

private fun <T : Any> Availability<T?>.orNotReported(): Availability<T> =
    when (this) {
        is Availability.Unavailable -> {
            this
        }

        is Availability.Available -> {
            val present = value
            if (present != null) {
                Availability.Available(present)
            } else {
                Availability.Unavailable(UnavailableReason.NotReportedByDevice)
            }
        }
    }
