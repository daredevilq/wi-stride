package io.github.daredevilq.wistride.core.wifi.source

import com.google.common.truth.Truth.assertThat
import io.github.daredevilq.wistride.core.wifi.rawLink
import io.github.daredevilq.wistride.core.wifi.rawWifiInfo
import org.junit.Test

class WifiNetworkTrackerTest {
    private val tracker = WifiNetworkTracker<String>()

    @Test
    fun noNetworkGivesNull() {
        assertThat(tracker.current()).isNull()
        assertThat(tracker.isEmpty()).isTrue()
    }

    @Test
    fun networkWithoutInfoYetHasNullParts() {
        tracker.onAvailable("a")
        assertThat(tracker.current()).isEqualTo(WifiNetworkInfo(wifiInfo = null, link = null))
    }

    @Test
    fun mostRecentlyAvailableNetworkWins() {
        val first = rawWifiInfo(rssi = -40)
        val second = rawWifiInfo(rssi = -70)
        tracker.onAvailable("a")
        tracker.onWifiInfo("a", first)
        tracker.onAvailable("b")
        tracker.onWifiInfo("b", second)
        assertThat(tracker.current()?.wifiInfo).isEqualTo(second)
        tracker.onLost("b")
        assertThat(tracker.current()?.wifiInfo).isEqualTo(first)
        tracker.onLost("a")
        assertThat(tracker.current()).isNull()
    }

    @Test
    fun linkAndInfoArrivingBeforeAvailableCreateTheEntry() {
        tracker.onLinkInfo("a", rawLink())
        assertThat(tracker.current()?.link).isEqualTo(rawLink())
    }

    @Test
    fun polledInfoFillsInUntilTheNetworkIsLost() {
        val polled = rawWifiInfo(rssi = -61)
        tracker.onAvailable("a")
        tracker.setPolledInfo(polled)
        assertThat(tracker.current()?.wifiInfo).isEqualTo(polled)
        tracker.onLost("a")
        tracker.onAvailable("b")
        assertThat(tracker.current()?.wifiInfo).isNull()
    }
}
