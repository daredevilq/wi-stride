package io.github.daredevilq.wistride.feature.ping

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.Locale

class PingHelpersTest {
    @Test
    fun splitAtLossesBreaksRunsAtNulls() {
        val runs = splitAtLosses(listOf(1.0, 2.0, null, null, 3.0, null, 4.0, 5.0))
        assertThat(runs.map { run -> run.map { it.index } })
            .containsExactly(listOf(0, 1), listOf(4), listOf(6, 7))
            .inOrder()
        assertThat(runs[2].map { it.value }).containsExactly(4.0, 5.0).inOrder()
    }

    @Test
    fun splitAtLossesHandlesEmptyAndAllLost() {
        assertThat(splitAtLosses(emptyList())).isEmpty()
        assertThat(splitAtLosses(listOf(null, null))).isEmpty()
    }

    @Test
    fun millisecondsUseOneDecimalBelowTenAndWholeNumbersAbove() {
        assertThat(formatMs(0.04, Locale.US)).isEqualTo("0.0")
        assertThat(formatMs(9.94, Locale.US)).isEqualTo("9.9")
        assertThat(formatMs(12.4, Locale.US)).isEqualTo("12")
        assertThat(formatMs(1234.0, Locale.US)).isEqualTo("1,234")
    }

    @Test
    fun numbersFollowTheLocale() {
        val polish = Locale.forLanguageTag("pl-PL")
        assertThat(formatMs(3.26, polish)).isEqualTo("3,3")
        assertThat(formatPercent(33.333, polish)).isEqualTo("33,3")
        assertThat(formatSeconds(500L, polish)).isEqualTo("0,5")
    }

    @Test
    fun percentAndSecondsDropNeedlessDecimals() {
        assertThat(formatPercent(0.0, Locale.US)).isEqualTo("0")
        assertThat(formatPercent(100.0, Locale.US)).isEqualTo("100")
        assertThat(formatSeconds(2_000L, Locale.US)).isEqualTo("2")
    }
}
