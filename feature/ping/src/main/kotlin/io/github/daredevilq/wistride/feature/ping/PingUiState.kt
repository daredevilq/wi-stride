package io.github.daredevilq.wistride.feature.ping

import androidx.compose.runtime.Immutable
import io.github.daredevilq.wistride.core.model.permission.PermissionStatus
import io.github.daredevilq.wistride.core.model.ping.LatencyUnavailableReason
import io.github.daredevilq.wistride.core.model.ping.PingMethod
import io.github.daredevilq.wistride.core.model.ping.PingStats

/** Everything the "Ping" screen renders. */
@Immutable
sealed interface PingUiState {
    /** Settings or the first permission reading have not arrived yet. */
    data object Loading : PingUiState

    /** The target list is empty; the user can add a target or restore the defaults. */
    @Immutable
    data class Empty(
        val intervalMs: Long,
    ) : PingUiState

    /** Every target reports that no Wi-Fi network is connected, so the list would only repeat that. */
    @Immutable
    data class NoWifi(
        val intervalMs: Long,
        /** Size of the target list, to know whether another target can still be added. */
        val targetCount: Int,
    ) : PingUiState

    @Immutable
    data class Content(
        val targets: List<TargetUi>,
        val intervalMs: Long,
        /** Id of the target shown in the detail view; always one of [targets], or null. */
        val selectedId: String?,
        /** Status of the local-network permission, for targets that report `NeedsPermission`. */
        val permission: PermissionStatus,
    ) : PingUiState
}

/** One measured target as the list card and the detail view show it. */
@Immutable
data class TargetUi(
    val id: String,
    /** Host name or address as the user entered it; null for the gateway, which has a localized name. */
    val host: String?,
    /** Null until the monitor reports which method it uses. */
    val method: PingMethod?,
    /** Resolved IP literal the probes go to. */
    val address: String?,
    /** Over the last `PING_STATS_WINDOW` probes. */
    val stats: PingStats,
    /** Most recent probe; null when it was lost or none was made yet. */
    val currentMs: Double?,
    /** Last 30 probes, oldest first, null = lost. */
    val sparkline: List<Double?>,
    /** Last 120 probes, oldest first, null = lost. */
    val chart: List<Double?>,
    /** True for host names: only they are resolved, so only they have a DNS time. */
    val resolvesDns: Boolean,
    val dnsMs: Double?,
    /** Non-null while the target cannot be measured; numbers are then not shown. */
    val unavailable: LatencyUnavailableReason?,
)
