package io.github.daredevilq.wistride.domain.network

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PingStatsCalculatorTest {
    private val tolerance = 1e-9

    @Test
    fun emptyWindowHasNoValues() {
        val stats = computePingStats(emptyList())
        assertThat(stats.sent).isEqualTo(0)
        assertThat(stats.received).isEqualTo(0)
        assertThat(stats.minMs).isNull()
        assertThat(stats.avgMs).isNull()
        assertThat(stats.maxMs).isNull()
        assertThat(stats.jitterMs).isNull()
        assertThat(stats.lossPercent).isNull()
    }

    @Test
    fun singleAnswerHasNoJitter() {
        val stats = computePingStats(listOf(12.0))
        assertThat(stats.minMs).isEqualTo(12.0)
        assertThat(stats.avgMs).isEqualTo(12.0)
        assertThat(stats.maxMs).isEqualTo(12.0)
        assertThat(stats.jitterMs).isNull()
        assertThat(stats.lossPercent).isEqualTo(0.0)
    }

    @Test
    fun allLostReportsFullLossAndNoRtt() {
        val stats = computePingStats(listOf(null, null, null))
        assertThat(stats.sent).isEqualTo(3)
        assertThat(stats.received).isEqualTo(0)
        assertThat(stats.minMs).isNull()
        assertThat(stats.avgMs).isNull()
        assertThat(stats.maxMs).isNull()
        assertThat(stats.jitterMs).isNull()
        assertThat(stats.lossPercent).isEqualTo(100.0)
    }

    @Test
    fun minAvgMaxAndJitterOverAnsweredProbes() {
        val stats = computePingStats(listOf(10.0, 20.0, 15.0, 15.0))
        assertThat(stats.minMs).isEqualTo(10.0)
        assertThat(stats.maxMs).isEqualTo(20.0)
        assertThat(stats.avgMs).isWithin(tolerance).of(15.0)
        // (10 + 5 + 0) / 3
        assertThat(stats.jitterMs).isWithin(tolerance).of(5.0)
        assertThat(stats.lossPercent).isEqualTo(0.0)
    }

    @Test
    fun lostProbesAreSkippedForJitterButCountedForLoss() {
        val stats = computePingStats(listOf(10.0, null, 30.0, null, null, 20.0))
        assertThat(stats.sent).isEqualTo(6)
        assertThat(stats.received).isEqualTo(3)
        // |30 - 10| = 20, |20 - 30| = 10 -> 15
        assertThat(stats.jitterMs).isWithin(tolerance).of(15.0)
        assertThat(stats.lossPercent).isWithin(tolerance).of(50.0)
        assertThat(stats.avgMs).isWithin(tolerance).of(20.0)
    }

    @Test
    fun twoAnswersAmongLossesGiveJitterOfTheirDifference() {
        val stats = computePingStats(listOf(null, 8.0, null, 11.0))
        assertThat(stats.jitterMs).isWithin(tolerance).of(3.0)
    }

    @Test
    fun lossPercentIsFractionOfSent() {
        val window = List(PING_STATS_WINDOW) { if (it % 3 == 0) null else 5.0 }
        assertThat(computePingStats(window).lossPercent).isWithin(tolerance).of(100.0 / 3)
    }
}
