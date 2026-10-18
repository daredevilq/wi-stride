package io.github.daredevilq.wistride.domain.wifi

private const val RSSI_MAX_DBM = 0

/**
 * Strips one pair of surrounding double quotes (Android quotes UTF-8 SSIDs). Returns `null` for
 * null, empty (also `""`) and the redacted `<unknown ssid>`. Unquoted (hex) values are kept as is.
 */
fun normalizeSsid(raw: String?): String? {
    if (raw == null) return null
    val quoted = raw.length >= 2 && raw.startsWith('"') && raw.endsWith('"')
    val unquoted = if (quoted) raw.substring(1, raw.length - 1) else raw
    return unquoted.takeUnless { it.isEmpty() || it == AndroidWifiConstants.UNKNOWN_SSID }
}

/** Lower-cases [raw]; `null` for null and the redacted `02:00:00:00:00:00` placeholder. */
fun normalizeBssid(raw: String?): String? {
    val lower = raw?.lowercase() ?: return null
    return lower.takeUnless { it == AndroidWifiConstants.REDACTED_BSSID }
}

/** [raw] if it is a plausible RSSI (-126..0 dBm); `null` for `INVALID_RSSI` (-127) and nonsense. */
fun rssiOrNull(raw: Int): Int? = raw.takeIf { it > AndroidWifiConstants.INVALID_RSSI && it <= RSSI_MAX_DBM }

/** [raw] in Mbps if positive; `null` for `LINK_SPEED_UNKNOWN` (-1), zero and negatives. */
fun linkSpeedOrNull(raw: Int): Int? = raw.takeIf { it > 0 }
