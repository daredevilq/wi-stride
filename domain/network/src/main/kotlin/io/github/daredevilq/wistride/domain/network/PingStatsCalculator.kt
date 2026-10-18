package io.github.daredevilq.wistride.domain.network

import io.github.daredevilq.wistride.core.model.ping.PingStats
import kotlin.math.abs

/** Number of most recent probes the statistics are computed over (about a minute at the default 1 s interval). */
const val PING_STATS_WINDOW = 60

private const val PERCENT = 100.0

/**
 * Statistics over [window], the most recent probes in chronological order (null = no answer).
 *
 * - `min` / `avg` / `max`: over the answered probes.
 * - `jitter` = mean of `|rttᵢ − rttᵢ₋₁|` over consecutive *answered* probes (lost probes are skipped,
 *   so a loss does not count as a latency jump); null with fewer than two answers. This is the plain
 *   mean deviation that desktop `ping` tools report, not the RFC 3550 running estimator
 *   `J += (|D| − J) / 16`, whose smoothing carries state from before the window and so cannot be
 *   reproduced from the window alone.
 * - `lossPercent` = `(sent − received) / sent × 100`; null when nothing was sent.
 *
 * RTT fields stay null while no probe was answered, never a fake zero.
 */
fun computePingStats(window: List<Double?>): PingStats {
    val answered = window.filterNotNull()
    val sent = window.size
    val received = answered.size
    val jitter =
        if (received < 2) {
            null
        } else {
            answered.zipWithNext { previous, next -> abs(next - previous) }.average()
        }
    return PingStats(
        sent = sent,
        received = received,
        minMs = answered.minOrNull(),
        avgMs = if (received == 0) null else answered.average(),
        maxMs = answered.maxOrNull(),
        jitterMs = jitter,
        lossPercent = if (sent == 0) null else (sent - received) * PERCENT / sent,
    )
}
