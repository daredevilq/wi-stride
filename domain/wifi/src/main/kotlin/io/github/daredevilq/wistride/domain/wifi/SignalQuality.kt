package io.github.daredevilq.wistride.domain.wifi

import io.github.daredevilq.wistride.core.model.wifi.SignalLevel
import io.github.daredevilq.wistride.core.model.wifi.SignalQuality

/** RSSI at and below which the percentage is 0. */
const val RSSI_EMPTY_DBM = -100

/** Lower bounds (inclusive) of each [SignalLevel]; anything below [LEVEL_WEAK_MIN_DBM] is Poor. */
const val LEVEL_EXCELLENT_MIN_DBM = -50
const val LEVEL_GOOD_MIN_DBM = -60
const val LEVEL_FAIR_MIN_DBM = -70
const val LEVEL_WEAK_MIN_DBM = -80

private const val PERCENT_MAX = 100
private const val PERCENT_PER_DBM = 2

/**
 * Converts [rssiDbm] to a human-readable quality.
 *
 * Percent is the common vendor mapping (Windows / legacy Android `calculateSignalLevel`):
 * `percent = clamp(2 * (rssi + 100), 0, 100)`, so -50 dBm and above is 100 % and -100 dBm and
 * below is 0 %. Level thresholds: >= -50 Excellent, >= -60 Good, >= -70 Fair, >= -80 Weak,
 * otherwise Poor (a widely used rule of thumb for 2.4/5 GHz links).
 */
fun signalQuality(rssiDbm: Int): SignalQuality {
    val percent = (PERCENT_PER_DBM * (rssiDbm - RSSI_EMPTY_DBM)).coerceIn(0, PERCENT_MAX)
    val level =
        when {
            rssiDbm >= LEVEL_EXCELLENT_MIN_DBM -> SignalLevel.Excellent
            rssiDbm >= LEVEL_GOOD_MIN_DBM -> SignalLevel.Good
            rssiDbm >= LEVEL_FAIR_MIN_DBM -> SignalLevel.Fair
            rssiDbm >= LEVEL_WEAK_MIN_DBM -> SignalLevel.Weak
            else -> SignalLevel.Poor
        }
    return SignalQuality(percent, level)
}
