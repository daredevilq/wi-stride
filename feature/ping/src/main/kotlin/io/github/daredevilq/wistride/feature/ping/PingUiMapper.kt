package io.github.daredevilq.wistride.feature.ping

import io.github.daredevilq.wistride.core.model.permission.PermissionStatus
import io.github.daredevilq.wistride.core.model.ping.LatencyEvent
import io.github.daredevilq.wistride.core.model.ping.LatencyUnavailableReason
import io.github.daredevilq.wistride.core.model.ping.PingMethod
import io.github.daredevilq.wistride.core.model.ping.PingSettings
import io.github.daredevilq.wistride.core.model.ping.PingTarget
import io.github.daredevilq.wistride.domain.network.PING_STATS_WINDOW
import io.github.daredevilq.wistride.domain.network.computePingStats
import io.github.daredevilq.wistride.domain.network.isIpLiteral

internal const val CHART_HISTORY_SIZE = 120
internal const val SPARKLINE_SIZE = 30

/** Raw per-target measurement state accumulated from [LatencyEvent]s. */
internal data class TargetRuntime(
    val method: PingMethod? = null,
    val address: String? = null,
    /** Last up to [CHART_HISTORY_SIZE] probes, oldest first, null = lost. */
    val probes: List<Double?> = emptyList(),
    val dnsMs: Double? = null,
    val unavailable: LatencyUnavailableReason? = null,
) {
    /** Any sign of life clears "unavailable": the monitor emits it only while conditions are bad. */
    fun apply(event: LatencyEvent): TargetRuntime =
        when (event) {
            is LatencyEvent.MethodSelected -> {
                copy(method = event.method, address = event.address, unavailable = null)
            }

            is LatencyEvent.Probe -> {
                copy(
                    probes = (probes + event.rttMs).takeLast(CHART_HISTORY_SIZE),
                    unavailable = null,
                )
            }

            is LatencyEvent.DnsResolved -> {
                copy(dnsMs = event.durationMs)
            }

            is LatencyEvent.Unavailable -> {
                copy(unavailable = event.reason)
            }
        }
}

internal fun TargetRuntime.toUi(target: PingTarget): TargetUi =
    TargetUi(
        id = target.id,
        host = (target as? PingTarget.Host)?.host,
        method = method,
        address = address,
        stats = computePingStats(probes.takeLast(PING_STATS_WINDOW)),
        currentMs = probes.lastOrNull(),
        sparkline = probes.takeLast(SPARKLINE_SIZE),
        chart = probes,
        resolvesDns = (target as? PingTarget.Host)?.host?.let { !isIpLiteral(it) } ?: false,
        dnsMs = dnsMs,
        unavailable = unavailable,
    )

/** Screen-level state for [settings] and the per-target [runtimes] (targets without events yet are blank). */
internal fun toPingUiState(
    settings: PingSettings,
    runtimes: Map<String, TargetRuntime>,
    selectedId: String?,
    permission: PermissionStatus,
): PingUiState {
    val targets = settings.targets.map { (runtimes[it.id] ?: TargetRuntime()).toUi(it) }
    return when {
        targets.isEmpty() -> {
            PingUiState.Empty(settings.intervalMs)
        }

        targets.all { it.unavailable == LatencyUnavailableReason.NoWifi } -> {
            PingUiState.NoWifi(settings.intervalMs, targets.size)
        }

        else -> {
            PingUiState.Content(
                targets = targets,
                intervalMs = settings.intervalMs,
                selectedId = selectedId?.takeIf { id -> targets.any { it.id == id } },
                permission = permission,
            )
        }
    }
}
