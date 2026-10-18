package io.github.daredevilq.wistride.feature.ping

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings

/**
 * Opens the Wi-Fi panel. Some OEM builds lack it, so on [ActivityNotFoundException] it falls back to
 * the Wi-Fi settings and finally the main settings. (Copied from the Now feature: features must not
 * depend on each other.)
 */
internal fun Context.openWifiPanel() {
    val candidates = listOf(Settings.Panel.ACTION_WIFI, Settings.ACTION_WIFI_SETTINGS, Settings.ACTION_SETTINGS)
    for (candidate in candidates) {
        try {
            startActivity(Intent(candidate).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        } catch (_: ActivityNotFoundException) {
            // Try the next, more general screen.
        }
    }
}
