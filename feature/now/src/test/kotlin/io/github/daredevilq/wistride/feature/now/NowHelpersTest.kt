package io.github.daredevilq.wistride.feature.now

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NowHelpersTest {
    @Test
    fun androidVersionNamesMapSdkLevels() {
        val expected =
            mapOf(
                29 to "10",
                30 to "11",
                31 to "12",
                32 to "12",
                33 to "13",
                34 to "14",
                35 to "15",
                36 to "16",
            )
        expected.forEach { (sdk, name) -> assertThat(androidVersionName(sdk)).isEqualTo(name) }
    }

    @Test
    fun summaryIgnoresNullsAndReportsCurrent() {
        val summary = listOf(-70, null, -50, -60).summarize()!!
        assertThat(summary).isEqualTo(RssiSummary(current = -60, min = -70, max = -50))
    }

    @Test
    fun summaryCurrentIsNullWhenLastSampleMissing() {
        assertThat(listOf(-70, null).summarize()!!.current).isNull()
    }

    @Test
    fun summaryIsNullWithoutReadings() {
        assertThat(emptyList<Int?>().summarize()).isNull()
        assertThat(listOf(null, null).summarize()).isNull()
    }

    @Test
    fun chartPointsAreRightAlignedAndSkipNulls() {
        val (x, y) = listOf(-70, null, -60).chartPoints(windowSize = 5)
        assertThat(x).containsExactly(2, 4).inOrder()
        assertThat(y).containsExactly(-70, -60).inOrder()
    }
}
