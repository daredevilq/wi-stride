package io.github.daredevilq.wistride.domain.wifi

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import io.github.daredevilq.wistride.core.model.wifi.ChannelWidth
import io.github.daredevilq.wistride.core.model.wifi.WifiBand
import io.github.daredevilq.wistride.core.model.wifi.WifiChannel
import org.junit.Test

class WifiChannelsTest {
    private val valid =
        listOf(
            // 2.4 GHz edges, ch 14
            Triple(2412, WifiBand.Ghz2Point4, 1),
            Triple(2437, WifiBand.Ghz2Point4, 6),
            Triple(2472, WifiBand.Ghz2Point4, 13),
            Triple(2484, WifiBand.Ghz2Point4, 14),
            // 5 GHz edges
            Triple(5005, WifiBand.Ghz5, 1),
            Triple(5035, WifiBand.Ghz5, 7),
            Triple(5180, WifiBand.Ghz5, 36),
            Triple(5500, WifiBand.Ghz5, 100),
            Triple(5885, WifiBand.Ghz5, 177),
            Triple(5920, WifiBand.Ghz5, 184),
            // 6 GHz
            Triple(5935, WifiBand.Ghz6, 2),
            Triple(5955, WifiBand.Ghz6, 1),
            Triple(5975, WifiBand.Ghz6, 5),
            Triple(6415, WifiBand.Ghz6, 93),
            Triple(7115, WifiBand.Ghz6, 233),
        )

    private val invalid =
        listOf(
            Int.MIN_VALUE,
            -2412,
            -1,
            0,
            1,
            2411,
            2413,
            2418,
            2473,
            2477,
            2483,
            2485,
            2490,
            5000,
            5001,
            5003,
            5922,
            5925,
            5930,
            5934,
            5936,
            5940,
            5950,
            5954,
            5956,
            7116,
            7120,
            7125,
            58320,
            60480,
            Int.MAX_VALUE,
        )

    @Test
    fun `valid frequencies resolve to band and channel`() {
        valid.forEach { (freq, band, number) ->
            assertWithMessage("freq $freq")
                .that(WifiChannels.fromFrequency(freq))
                .isEqualTo(WifiChannel(band, number, freq))
            assertWithMessage("band of $freq").that(WifiChannels.bandOf(freq)).isEqualTo(band)
        }
    }

    @Test
    fun `invalid frequencies resolve to null`() {
        invalid.forEach { freq ->
            assertWithMessage("freq $freq").that(WifiChannels.fromFrequency(freq)).isNull()
            assertWithMessage("band of $freq").that(WifiChannels.bandOf(freq)).isNull()
        }
    }

    @Test
    fun `bandOf is consistent with fromFrequency over a wide sweep`() {
        for (freq in -10..8000) {
            assertWithMessage("freq $freq")
                .that(WifiChannels.bandOf(freq))
                .isEqualTo(WifiChannels.fromFrequency(freq)?.band)
        }
    }

    @Test
    fun `occupied range is centre plus or minus half the width`() {
        val cases =
            listOf(
                Triple(2437, ChannelWidth.Mhz20, 2427..2447),
                Triple(5190, ChannelWidth.Mhz40, 5170..5210),
                Triple(5210, ChannelWidth.Mhz80, 5170..5250),
                Triple(5250, ChannelWidth.Mhz160, 5170..5330),
                Triple(5210, ChannelWidth.Mhz80Plus80, 5170..5250),
                Triple(6105, ChannelWidth.Mhz320, 5945..6265),
            )
        cases.forEach { (center, width, expected) ->
            assertWithMessage("$center $width")
                .that(WifiChannels.occupiedRangeMhz(center, width))
                .isEqualTo(expected)
        }
        assertThat(WifiChannels.occupiedRangeMhz(5210, ChannelWidth.Mhz80).count()).isEqualTo(81)
    }
}
