package io.github.daredevilq.wistride.core.wifi.source

import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class AndroidSourcesRobolectricTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val wifiManager = context.getSystemService(WifiManager::class.java)
    private val locationManager = context.getSystemService(LocationManager::class.java)

    private fun stateSource() = AndroidWifiStateSource(context, wifiManager, locationManager)

    private fun wifiStateBroadcast(state: Int) {
        context.sendBroadcast(
            Intent(WifiManager.WIFI_STATE_CHANGED_ACTION).putExtra(WifiManager.EXTRA_WIFI_STATE, state),
        )
        shadowOf(android.os.Looper.getMainLooper()).idle()
    }

    @Test
    fun wifiEnabledFollowsBroadcasts() =
        runTest {
            @Suppress("DEPRECATION")
            wifiManager.isWifiEnabled = false
            stateSource().wifiEnabled.test {
                assertThat(awaitItem()).isFalse()
                wifiStateBroadcast(WifiManager.WIFI_STATE_ENABLING)
                expectNoEvents()
                wifiStateBroadcast(WifiManager.WIFI_STATE_ENABLED)
                assertThat(awaitItem()).isTrue()
                wifiStateBroadcast(WifiManager.WIFI_STATE_DISABLING)
                assertThat(awaitItem()).isFalse()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun airplaneModeFollowsSettingsOnBroadcast() =
        runTest {
            Settings.Global.putInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0)
            stateSource().airplaneModeOn.test {
                assertThat(awaitItem()).isFalse()
                Settings.Global.putInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 1)
                context.sendBroadcast(Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED))
                shadowOf(android.os.Looper.getMainLooper()).idle()
                assertThat(awaitItem()).isTrue()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun receiverIsUnregisteredAfterCancel() =
        runTest {
            val source = stateSource()
            source.wifiEnabled.test { cancelAndIgnoreRemainingEvents() }
            val ours =
                shadowOf(context as android.app.Application).registeredReceivers.filter {
                    "wistride" in
                        it.broadcastReceiver.javaClass.name
                }
            assertThat(ours).isEmpty()
        }

    @Test
    fun scanResultsAreMappedAndRefreshedOnBroadcast() =
        runTest {
            val source = AndroidScanResultsSource(context, wifiManager)
            shadowOf(wifiManager).setScanResults(emptyList())
            source.scanResults.test {
                assertThat(awaitItem()).isEmpty()
                shadowOf(wifiManager).setScanResults(listOf(scanResult("aa:bb:cc:dd:ee:ff", -48, 5180)))
                context.sendBroadcast(Intent(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
                shadowOf(android.os.Looper.getMainLooper()).idle()
                val mapped = awaitItem().single()
                assertThat(mapped.bssid).isEqualTo("aa:bb:cc:dd:ee:ff")
                assertThat(mapped.rssi).isEqualTo(-48)
                assertThat(mapped.frequencyMhz).isEqualTo(5180)
                cancelAndIgnoreRemainingEvents()
            }
        }

    private fun scanResult(
        bssid: String,
        level: Int,
        frequency: Int,
    ): ScanResult {
        val result =
            ScanResult::class.java
                .getDeclaredConstructor()
                .apply { isAccessible = true }
                .newInstance()
        result.BSSID = bssid
        result.level = level
        result.frequency = frequency
        return result
    }
}
