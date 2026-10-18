package io.github.daredevilq.wistride.feature.now

import androidx.compose.runtime.Immutable
import io.github.daredevilq.wistride.core.model.Availability
import io.github.daredevilq.wistride.core.model.permission.PermissionStatus
import io.github.daredevilq.wistride.core.model.wifi.ChannelWidth
import io.github.daredevilq.wistride.core.model.wifi.IpConfiguration
import io.github.daredevilq.wistride.core.model.wifi.SignalQuality
import io.github.daredevilq.wistride.core.model.wifi.WifiBand
import io.github.daredevilq.wistride.core.model.wifi.WifiGeneration
import io.github.daredevilq.wistride.core.model.wifi.WifiSecurity
import io.github.daredevilq.wistride.core.model.wifi.WifiStandard

/** Everything the "Now" screen renders. */
@Immutable
sealed interface NowUiState {
    /** No snapshot has arrived yet. */
    data object Loading : NowUiState

    @Immutable
    data class Loaded(
        val link: LinkStatus,
        val permission: PermissionStatus,
        /** True when the permission is granted but system location services are off (values are redacted). */
        val locationServicesOff: Boolean,
        /** Last up to 60 one-per-second RSSI samples, oldest first; null = not connected / unknown. */
        val rssiHistory: List<Int?>,
    ) : NowUiState
}

/** What the Wi-Fi radio is doing; mirrors the model state with derived values attached. */
@Immutable
sealed interface LinkStatus {
    data object AirplaneMode : LinkStatus

    data object WifiOff : LinkStatus

    data object Disconnected : LinkStatus

    data class Connected(
        val details: ConnectionDetails,
    ) : LinkStatus
}

/** Connected-network values plus those derived by `domain:wifi` (band, channel, quality, generation). */
@Immutable
data class ConnectionDetails(
    val ssid: Availability<String>,
    val bssid: Availability<String>,
    val rssiDbm: Availability<Int>,
    /** Null exactly when [rssiDbm] is unavailable. */
    val quality: SignalQuality?,
    val frequencyMhz: Availability<Int>,
    val band: Availability<WifiBand>,
    val channel: Availability<Int>,
    val channelWidth: Availability<ChannelWidth>,
    val generation: Availability<GenerationInfo>,
    val txLinkSpeedMbps: Availability<Int>,
    val rxLinkSpeedMbps: Availability<Int>,
    val maxTxLinkSpeedMbps: Availability<Int>,
    val maxRxLinkSpeedMbps: Availability<Int>,
    val security: Availability<WifiSecurity>,
    val ip: IpConfiguration,
)

/** Marketing name and IEEE amendment shown together, e.g. "Wi-Fi 6 (802.11ax)". */
@Immutable
data class GenerationInfo(
    val generation: WifiGeneration,
    val standard: WifiStandard,
)
