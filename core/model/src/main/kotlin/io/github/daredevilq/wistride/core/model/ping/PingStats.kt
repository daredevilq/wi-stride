package io.github.daredevilq.wistride.core.model.ping

/**
 * Statistics over a sliding window of probes. RTT values are null while no probe in the
 * window was answered — never a fake zero.
 *
 * @property sent probes in the window.
 * @property received answered probes in the window.
 * @property jitterMs mean absolute difference of consecutive answered RTTs.
 * @property lossPercent share of unanswered probes, 0–100; null while [sent] is 0.
 */
data class PingStats(
    val sent: Int,
    val received: Int,
    val minMs: Double?,
    val avgMs: Double?,
    val maxMs: Double?,
    val jitterMs: Double?,
    val lossPercent: Double?,
)
