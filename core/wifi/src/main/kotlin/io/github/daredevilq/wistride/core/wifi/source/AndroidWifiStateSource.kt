package io.github.daredevilq.wistride.core.wifi.source

import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.provider.Settings
import androidx.core.location.LocationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

internal class AndroidWifiStateSource
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val wifiManager: WifiManager,
        private val locationManager: LocationManager,
    ) : WifiStateSource {
        override val wifiEnabled: Flow<Boolean> =
            context.broadcastFlow(WifiManager.WIFI_STATE_CHANGED_ACTION) { intent ->
                val state = intent?.getIntExtra(WifiManager.EXTRA_WIFI_STATE, UNKNOWN_STATE) ?: UNKNOWN_STATE
                // The initial read and malformed broadcasts fall back to the manager's own state.
                (if (state == UNKNOWN_STATE) wifiManager.wifiState else state) == WifiManager.WIFI_STATE_ENABLED
            }

        override val airplaneModeOn: Flow<Boolean> =
            context.broadcastFlow(Intent.ACTION_AIRPLANE_MODE_CHANGED) {
                Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) != 0
            }

        override val locationEnabled: Flow<Boolean> =
            context.broadcastFlow(
                LocationManager.PROVIDERS_CHANGED_ACTION,
                LocationManager.MODE_CHANGED_ACTION,
            ) { LocationManagerCompat.isLocationEnabled(locationManager) }

        private companion object {
            const val UNKNOWN_STATE = -1
        }
    }
