package io.github.daredevilq.wistride.core.network

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.net.InetAddress

class WifiPathTrackerTest {
    private val gateway = InetAddress.getByName("192.168.1.1")
    private val link = WifiLink(gateway, listOf(LinkPrefix(InetAddress.getByName("192.168.1.20"), 24)))
    private val tracker = WifiPathTracker<String> { FakeNetworkHandle(name = it) }

    @Test
    fun `no path until link properties arrive`() {
        tracker.onWifiAvailable("wifi")
        assertThat(tracker.isEmpty()).isFalse()
        assertThat(tracker.current()).isNull()
    }

    @Test
    fun `path carries gateway and addresses`() {
        tracker.onWifiAvailable("wifi")
        tracker.onLink("wifi", link)
        val path = tracker.current()!!
        assertThat(path.gateway).isEqualTo(gateway)
        assertThat(path.linkAddresses).isEqualTo(link.addresses)
        assertThat(path.isDefault).isFalse()
    }

    @Test
    fun `is default only when default network is the Wi-Fi network`() {
        tracker.onWifiAvailable("wifi")
        tracker.onLink("wifi", link)
        tracker.onDefaultAvailable("cell")
        assertThat(tracker.current()!!.isDefault).isFalse()
        tracker.onDefaultAvailable("wifi")
        assertThat(tracker.current()!!.isDefault).isTrue()
        tracker.onDefaultLost("wifi")
        assertThat(tracker.current()!!.isDefault).isFalse()
    }

    @Test
    fun `stale default loss is ignored`() {
        tracker.onWifiAvailable("wifi")
        tracker.onLink("wifi", link)
        tracker.onDefaultAvailable("wifi")
        tracker.onDefaultLost("cell")
        assertThat(tracker.current()!!.isDefault).isTrue()
    }

    @Test
    fun `losing the Wi-Fi network empties the tracker`() {
        tracker.onWifiAvailable("wifi")
        tracker.onLink("wifi", link)
        tracker.onWifiLost("wifi")
        assertThat(tracker.isEmpty()).isTrue()
        assertThat(tracker.current()).isNull()
    }
}
