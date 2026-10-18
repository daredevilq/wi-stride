package io.github.daredevilq.wistride.domain.wifi

import io.github.daredevilq.wistride.core.model.wifi.ChannelWidth
import io.github.daredevilq.wistride.core.model.wifi.WifiBand
import io.github.daredevilq.wistride.core.model.wifi.WifiChannel

/**
 * Channel-number maths from IEEE 802.11 Annex E (global operating classes).
 *
 * Only frequencies that lie exactly on a channel grid are accepted; anything else (60 GHz,
 * off-grid values, zero, negatives) yields `null`.
 */
object WifiChannels {
    private const val GRID_STEP_MHZ = 5

    private const val BAND_2G_FIRST_MHZ = 2412
    private const val BAND_2G_LAST_REGULAR_MHZ = 2472
    private const val BAND_2G_BASE_MHZ = 2407
    private const val CHANNEL_14_MHZ = 2484
    private const val CHANNEL_14_NUMBER = 14

    private const val BAND_5G_BASE_MHZ = 5000
    private const val BAND_5G_UPPER_EXCLUSIVE_MHZ = 5925

    private const val BAND_6G_BASE_MHZ = 5950
    private const val BAND_6G_FIRST_MHZ = 5955
    private const val BAND_6G_LAST_MHZ = 7115
    private const val CHANNEL_2_6G_MHZ = 5935
    private const val CHANNEL_2_6G_NUMBER = 2

    private const val SEGMENT_80_PLUS_80_MHZ = 80

    /**
     * Resolves [frequencyMhz] (centre frequency) to a channel.
     *
     * - 2.4 GHz: `n = (f - 2407) / 5` for 2412..2472 (ch 1-13); 2484 is ch 14.
     * - 5 GHz: `n = (f - 5000) / 5` for 5000 < f < 5925 (up to U-NII-4 ch 177; 4.9 GHz public-safety
     *   channels are not supported; 5925 and above belong to 6 GHz).
     * - 6 GHz: `n = (f - 5950) / 5` for 5955..7115 (ch 1-233); 5935 is the special ch 2.
     */
    fun fromFrequency(frequencyMhz: Int): WifiChannel? {
        val band = bandOf(frequencyMhz) ?: return null
        val number =
            when (band) {
                WifiBand.Ghz2Point4 -> {
                    if (frequencyMhz == CHANNEL_14_MHZ) {
                        CHANNEL_14_NUMBER
                    } else {
                        (frequencyMhz - BAND_2G_BASE_MHZ) / GRID_STEP_MHZ
                    }
                }

                WifiBand.Ghz5 -> {
                    (frequencyMhz - BAND_5G_BASE_MHZ) / GRID_STEP_MHZ
                }

                WifiBand.Ghz6 -> {
                    if (frequencyMhz == CHANNEL_2_6G_MHZ) {
                        CHANNEL_2_6G_NUMBER
                    } else {
                        (frequencyMhz - BAND_6G_BASE_MHZ) / GRID_STEP_MHZ
                    }
                }
            }
        return WifiChannel(band, number, frequencyMhz)
    }

    /** Band of [frequencyMhz]; `null` exactly when [fromFrequency] returns `null`. */
    fun bandOf(frequencyMhz: Int): WifiBand? =
        when {
            frequencyMhz == CHANNEL_14_MHZ -> WifiBand.Ghz2Point4

            frequencyMhz in BAND_2G_FIRST_MHZ..BAND_2G_LAST_REGULAR_MHZ &&
                onGrid(frequencyMhz, BAND_2G_BASE_MHZ) -> WifiBand.Ghz2Point4

            frequencyMhz > BAND_5G_BASE_MHZ &&
                frequencyMhz < BAND_5G_UPPER_EXCLUSIVE_MHZ &&
                onGrid(frequencyMhz, BAND_5G_BASE_MHZ) -> WifiBand.Ghz5

            frequencyMhz == CHANNEL_2_6G_MHZ -> WifiBand.Ghz6

            frequencyMhz in BAND_6G_FIRST_MHZ..BAND_6G_LAST_MHZ &&
                onGrid(frequencyMhz, BAND_6G_BASE_MHZ) -> WifiBand.Ghz6

            else -> null
        }

    /**
     * Frequency span occupied by a channel: `centre - width/2 .. centre + width/2` (inclusive).
     * For [ChannelWidth.Mhz80Plus80] only the primary 80 MHz segment (centre +/- 40) is
     * returned because the second segment's centre is not known here.
     */
    fun occupiedRangeMhz(
        centerFrequencyMhz: Int,
        width: ChannelWidth,
    ): IntRange {
        val spanMhz = if (width == ChannelWidth.Mhz80Plus80) SEGMENT_80_PLUS_80_MHZ else width.totalMhz
        val half = spanMhz / 2
        return (centerFrequencyMhz - half)..(centerFrequencyMhz + half)
    }

    private fun onGrid(
        frequencyMhz: Int,
        baseMhz: Int,
    ): Boolean = (frequencyMhz - baseMhz) % GRID_STEP_MHZ == 0
}
