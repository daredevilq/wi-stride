package io.github.daredevilq.wistride.domain.network

/** One line of `ping` output, reduced to what latency measurement needs. */
sealed interface PingLine {
    /** An echo reply; [duplicate] is true for lines marked `(DUP!)`. */
    data class Reply(
        val seq: Int,
        val rttMs: Double,
        val ttl: Int?,
        val duplicate: Boolean,
    ) : PingLine

    /** `-O` report: no reply to [seq] yet (printed after the per-probe timeout). */
    data class NoAnswer(
        val seq: Int,
    ) : PingLine

    /** An ICMP error ("Destination Host Unreachable" and similar) reported for [seq]. */
    data class Unreachable(
        val seq: Int,
    ) : PingLine

    /** `ping: unknown host …`: the name could not be resolved. */
    data object UnknownHost : PingLine

    /** Banner, statistics, errors without a sequence number, blank lines. */
    data object Ignored : PingLine
}

private const val GROUP_SEQ = 1
private const val GROUP_TTL = 2
private const val GROUP_TIME = 3
private const val GROUP_DUP = 4

/**
 * Parser for iputils `ping -n -O` output (Android 10+; formats captured on a Samsung A53, API 36,
 * see `src/test/resources/ping`). Stateless and locale independent: numbers are parsed with
 * [String.toDouble], never with a locale-aware formatter.
 */
object PingOutputParser {
    // `-D` prefixes every line with `[epoch.micros] `; host names may appear as `name (ip)` before the colon.
    private const val PREFIX = """^(?:\[[\d.]+]\s+)?"""
    private val reply =
        Regex("""$PREFIX\d+ bytes from .*?icmp_seq=(\d+)(?:\s+ttl=(\d+))?\s+time[=<]([\d.]+)\s*ms(\s*\(DUP!\))?""")
    private val noAnswer = Regex("""${PREFIX}no answer yet for icmp_seq=(\d+)""")
    private val unreachable = Regex("""${PREFIX}From .*?icmp_seq=(\d+)\s+Destination .*Unreachable""")
    private val unknownHost = Regex("""^ping6?: unknown host""")

    /** Classifies one output line; never throws. */
    fun parsePingLine(line: String): PingLine {
        val text = line.trim()
        return parseReply(text)
            ?: noAnswer.find(text)?.let { PingLine.NoAnswer(it.groupValues[GROUP_SEQ].toInt()) }
            ?: unreachable.find(text)?.let { PingLine.Unreachable(it.groupValues[GROUP_SEQ].toInt()) }
            ?: if (unknownHost.containsMatchIn(text)) PingLine.UnknownHost else PingLine.Ignored
    }

    private fun parseReply(text: String): PingLine.Reply? {
        val groups = reply.find(text)?.groupValues ?: return null
        val seq = groups[GROUP_SEQ].toIntOrNull()
        // `time<1 ms` is an upper bound; reporting 1 ms keeps the value a safe over-estimate.
        val rtt = groups[GROUP_TIME].toDoubleOrNull()
        return if (seq == null || rtt == null) {
            null
        } else {
            PingLine.Reply(seq, rtt, groups[GROUP_TTL].toIntOrNull(), groups[GROUP_DUP].isNotEmpty())
        }
    }
}
