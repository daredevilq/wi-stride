package io.github.daredevilq.wistride.domain.network

/** Final result of one probe, reported exactly once per sequence number and in sequence order. */
sealed interface ProbeOutcome {
    val seq: Int

    data class Answered(
        override val seq: Int,
        val rttMs: Double,
    ) : ProbeOutcome

    data class Lost(
        override val seq: Int,
    ) : ProbeOutcome
}

private const val SEQ_WRAP_THRESHOLD = 32_768

/**
 * Turns [PingLine]s into per-probe [ProbeOutcome]s.
 *
 * Algorithm: keep `highest`, the greatest sequence number already reported (0 at start, `ping`
 * counts from 1).
 * - A [PingLine.Reply] with `seq > highest` marks every seq in `highest+1 until seq` as lost (without
 *   `-O` a timeout prints nothing, only a gap), then reports `seq` answered.
 * - [PingLine.NoAnswer] and [PingLine.Unreachable] with `seq > highest` report the gap and `seq` as lost.
 * - Anything with `seq <= highest` is dropped: duplicates (`DUP!`) and replies that arrive after
 *   `no answer yet` were already accounted for. A late reply keeps its probe lost on purpose: its RTT
 *   exceeded one interval, so counting it would hide the very latency spike the user wants to see.
 * - The 16-bit sequence counter wraps; a seq more than 32768 below `highest` is treated as a wrap and
 *   restarts the numbering.
 *
 * Not thread safe; use one instance per `ping` process.
 */
class PingSequenceTracker {
    private var highest = 0

    /** Outcomes completed by [line], in sequence order; empty for ignored or already reported lines. */
    fun onLine(line: PingLine): List<ProbeOutcome> =
        when (line) {
            is PingLine.Reply -> if (line.duplicate) emptyList() else advance(line.seq, line.rttMs)
            is PingLine.NoAnswer -> advance(line.seq, null)
            is PingLine.Unreachable -> advance(line.seq, null)
            PingLine.UnknownHost, PingLine.Ignored -> emptyList()
        }

    private fun advance(
        seq: Int,
        rttMs: Double?,
    ): List<ProbeOutcome> {
        if (highest - seq > SEQ_WRAP_THRESHOLD) highest = seq - 1
        if (seq <= highest) return emptyList()
        val skipped = ((highest + 1) until seq).map { ProbeOutcome.Lost(it) }
        highest = seq
        return skipped + if (rttMs != null) ProbeOutcome.Answered(seq, rttMs) else ProbeOutcome.Lost(seq)
    }
}
