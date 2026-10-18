package io.github.daredevilq.wistride.core.network

import com.google.common.truth.Truth.assertThat
import io.github.daredevilq.wistride.domain.network.isIpLiteral
import org.junit.Test
import java.net.InetAddress

class LocalAddressTest {
    private fun ip(literal: String) = InetAddress.getByName(literal)

    private val wifiLink = listOf(LinkPrefix(ip("203.0.113.7"), 24))

    @Test
    fun `private link-local and CGNAT ranges are local`() {
        listOf(
            "10.0.0.1",
            "10.255.255.254",
            "172.16.0.1",
            "172.31.255.254",
            "192.168.0.1",
            "192.168.255.254",
            "169.254.1.1",
            "100.64.0.1",
            "100.127.255.254",
            "fe80::1",
            "febf::1",
            "fc00::1",
            "fd12:3456::1",
        ).forEach { assertThat(isLocalAddress(ip(it), emptyList())).isTrue() }
    }

    @Test
    fun `public addresses and range neighbours are not local`() {
        listOf(
            "8.8.8.8",
            "1.1.1.1",
            "9.255.255.255",
            "11.0.0.1",
            "172.15.255.255",
            "172.32.0.1",
            "192.167.255.255",
            "192.169.0.1",
            "100.63.255.255",
            "100.128.0.1",
            "169.253.0.1",
            "2001:4860::8888",
            "fec0::1",
            "fe00::1",
        ).forEach { assertThat(isLocalAddress(ip(it), emptyList())).isFalse() }
    }

    @Test
    fun `address inside a Wi-Fi link prefix is local even when public`() {
        assertThat(isLocalAddress(ip("203.0.113.99"), wifiLink)).isTrue()
        assertThat(isLocalAddress(ip("203.0.114.1"), wifiLink)).isFalse()
    }

    @Test
    fun `address families never match each other's prefixes`() {
        assertThat(isLocalAddress(ip("2001:db8::1"), wifiLink)).isFalse()
    }

    @Test
    fun `parses IP literals without lookup`() {
        assertThat(parseIpLiteral("8.8.8.8")).isEqualTo(ip("8.8.8.8"))
        assertThat(parseIpLiteral("2001:4860::8888")).isEqualTo(ip("2001:4860::8888"))
    }

    @Test
    fun `numeric literals are exactly what the UI classifier calls literals`() {
        listOf("8.8.8.8", "2001:4860::8888", "google.com", "999.1.1.1", "1.2.3", "10.0.0").forEach {
            assertThat(parseIpLiteral(it) != null).isEqualTo(isIpLiteral(it))
        }
    }

    @Test
    fun `host names and malformed literals are not literals`() {
        listOf("google.com", "999.1.1.1", "1.2.3", "::zz::", "fe80::1%nosuchif0", "").forEach {
            assertThat(parseIpLiteral(it)).isNull()
        }
    }
}
