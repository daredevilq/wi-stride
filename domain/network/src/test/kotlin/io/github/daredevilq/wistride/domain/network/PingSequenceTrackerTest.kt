package io.github.daredevilq.wistride.domain.network

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PingSequenceTrackerTest {
    private fun run(resource: String): List<ProbeOutcome> {
        val tracker = PingSequenceTracker()
        val text = checkNotNull(javaClass.getResource("/ping/$resource")) { resource }.readText()
        return text.lines().flatMap { tracker.onLine(PingOutputParser.parsePingLine(it)) }
    }

    @Test
    fun `reachable host answers every probe`() {
        val outcomes = run("reachable.txt")
        assertThat(outcomes.map { it.seq }).containsExactly(1, 2, 3).inOrder()
        assertThat(outcomes.filterIsInstance<ProbeOutcome.Answered>().map { it.rttMs })
            .containsExactly(14.2, 14.5, 14.7)
            .inOrder()
    }

    @Test
    fun `no answer lines are lost probes`() {
        assertThat(run("no_answer.txt")).containsExactly(ProbeOutcome.Lost(1), ProbeOutcome.Lost(2)).inOrder()
    }

    @Test
    fun `host name and source address fixtures give one answer`() {
        assertThat(run("hostname.txt")).containsExactly(ProbeOutcome.Answered(1, 9.05))
        assertThat(run("from_address.txt")).containsExactly(ProbeOutcome.Answered(1, 9.74))
    }

    @Test
    fun `unknown host yields no outcomes`() {
        assertThat(run("unknown_host.txt")).isEmpty()
    }

    @Test
    fun `sequence gap marks skipped probes lost`() {
        assertThat(run("gap_without_O.txt"))
            .containsExactly(
                ProbeOutcome.Answered(1, 14.2),
                ProbeOutcome.Lost(2),
                ProbeOutcome.Lost(3),
                ProbeOutcome.Answered(4, 15.0),
                ProbeOutcome.Answered(5, 0.045),
            ).inOrder()
    }

    @Test
    fun `late reply and duplicates are ignored and nothing is reported twice`() {
        val outcomes = run("mixed_late_dup.txt")
        assertThat(outcomes)
            .containsExactly(
                ProbeOutcome.Answered(1, 14.2),
                ProbeOutcome.Lost(2),
                ProbeOutcome.Answered(3, 3100.0),
                ProbeOutcome.Lost(4),
                ProbeOutcome.Answered(5, 1.0),
            ).inOrder()
        assertThat(outcomes.map { it.seq }).containsNoDuplicates()
    }

    @Test
    fun `unreachable after gap reports gap and lost`() {
        val tracker = PingSequenceTracker()
        assertThat(tracker.onLine(PingLine.Unreachable(3)))
            .containsExactly(ProbeOutcome.Lost(1), ProbeOutcome.Lost(2), ProbeOutcome.Lost(3))
            .inOrder()
    }

    @Test
    fun `sequence wrap restarts numbering`() {
        val tracker = PingSequenceTracker()
        tracker.onLine(PingLine.Reply(65_535, 1.0, 64, duplicate = false))
        assertThat(tracker.onLine(PingLine.Reply(0, 2.0, 64, duplicate = false)))
            .containsExactly(ProbeOutcome.Answered(0, 2.0))
    }
}
