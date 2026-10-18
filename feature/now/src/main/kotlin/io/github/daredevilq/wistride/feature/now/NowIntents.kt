package io.github.daredevilq.wistride.feature.now

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings

/**
 * Starts a system settings screen for [action]. Some OEM builds lack individual panels, so on
 * [ActivityNotFoundException] it falls back to the Wi-Fi settings and finally the main settings.
 */
internal fun Context.openSettings(action: String) {
    val candidates = listOf(action, Settings.ACTION_WIFI_SETTINGS, Settings.ACTION_SETTINGS).distinct()
    for (candidate in candidates) {
        try {
            startActivity(Intent(candidate).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        } catch (_: ActivityNotFoundException) {
            // Try the next, more general screen.
        }
    }
}

internal fun Context.openWifiPanel() = openSettings(Settings.Panel.ACTION_WIFI)

internal fun Context.openAirplaneSettings() = openSettings(Settings.ACTION_AIRPLANE_MODE_SETTINGS)

internal fun Context.openLocationSettings() = openSettings(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
