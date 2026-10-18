package io.github.daredevilq.wistride.core.wifi.source

import android.content.Context
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

internal class AndroidScanResultsSource
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val wifiManager: WifiManager,
    ) : ScanResultsSource {
        override val scanResults: Flow<List<RawScanResult>> =
            context.broadcastFlow(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) { readScanResults() }

        // startScan is deprecated, but it is still the only public API to trigger a scan.
        @Suppress("DEPRECATION")
        override fun requestScan(): Boolean =
            try {
                wifiManager.startScan()
            } catch (_: SecurityException) {
                false
            }

        private fun readScanResults(): List<RawScanResult> =
            try {
                wifiManager.scanResults.map { it.toRaw() }
            } catch (_: SecurityException) {
                emptyList()
            }
    }

// SSID is deprecated in favour of wifiSsid (API 33+), but it is the only variant on API 29-32.
@Suppress("DEPRECATION")
internal fun ScanResult.toRaw(): RawScanResult =
    RawScanResult(
        bssid = BSSID.orEmpty(),
        ssid = SSID,
        rssi = level,
        frequencyMhz = frequency,
        channelWidth = channelWidth,
        centerFreq0Mhz = centerFreq0,
        centerFreq1Mhz = centerFreq1,
        timestampMicros = timestamp,
    )
