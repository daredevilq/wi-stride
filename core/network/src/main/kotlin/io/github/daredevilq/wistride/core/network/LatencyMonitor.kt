package io.github.daredevilq.wistride.core.network

import io.github.daredevilq.wistride.core.model.ping.LatencyEvent
import io.github.daredevilq.wistride.core.model.ping.PingTarget
import kotlinx.coroutines.flow.Flow

/**
 * Continuous latency measurement of one target over the Wi-Fi network.
 *
 * [monitor] is cold: probing (and any `ping` child process) starts on collection and is torn down
 * when the collector is cancelled, so nothing runs while the Ping screen is not visible. All
 * blocking work runs on the injected IO dispatcher.
 */
interface LatencyMonitor {
    /**
     * Probes [target] every [intervalMs]. Emits [LatencyEvent.MethodSelected] before the first probe
     * (and again on a method change), then [LatencyEvent.Probe]s; [LatencyEvent.Unavailable] while
     * the target cannot be measured (no Wi-Fi, missing permission, unknown host). Follows Wi-Fi
     * network changes on its own; permissions are checked when collection starts, so callers
     * re-collect after a permission grant (or to change [intervalMs]).
     */
    fun monitor(
        target: PingTarget,
        intervalMs: Long,
    ): Flow<LatencyEvent>
}
