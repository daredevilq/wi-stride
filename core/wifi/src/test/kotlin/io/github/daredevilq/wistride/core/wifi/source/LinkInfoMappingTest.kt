package io.github.daredevilq.wistride.core.wifi.source

import com.google.common.truth.Truth.assertThat
import io.github.daredevilq.wistride.core.model.wifi.IpAddressWithPrefix
import org.junit.Test
import java.net.InetAddress

class LinkInfoMappingTest {
    private fun ip(literal: String): InetAddress = InetAddress.getByName(literal)

    @Test
    fun splitsFamiliesAndKeepsPrefixes() {
        val info =
            rawLinkInfoOf(
                addresses = listOf(ip("192.168.1.23") to 24, ip("2001:db8::5") to 64, ip("fe80::1") to 64),
                defaultRouteGateways = listOf(ip("192.168.1.1")),
                dnsServers = listOf(ip("1.1.1.1"), ip("8.8.8.8")),
                mtu = 1500,
            )
        assertThat(info.ipv4).containsExactly(IpAddressWithPrefix("192.168.1.23", 24))
        assertThat(
            info.ipv6,
        ).containsExactly(
            IpAddressWithPrefix("2001:db8:0:0:0:0:0:5", 64),
            IpAddressWithPrefix("fe80:0:0:0:0:0:0:1", 64),
        )
        assertThat(info.gateway).isEqualTo("192.168.1.1")
        assertThat(info.dnsServers).containsExactly("1.1.1.1", "8.8.8.8").inOrder()
        assertThat(info.mtu).isEqualTo(1500)
    }

    @Test
    fun gatewayPrefersIpv4AndSkipsUnspecified() {
        val info =
            rawLinkInfoOf(
                addresses = emptyList(),
                defaultRouteGateways = listOf(ip("0.0.0.0"), ip("fe80::1"), ip("10.0.0.1")),
                dnsServers = emptyList(),
                mtu = 0,
            )
        assertThat(info.gateway).isEqualTo("10.0.0.1")
    }

    @Test
    fun ipv6OnlyGatewayAndNoGateway() {
        val v6 = rawLinkInfoOf(emptyList(), listOf(ip("fe80::1")), emptyList(), 0)
        assertThat(v6.gateway).isEqualTo("fe80:0:0:0:0:0:0:1")
        val none = rawLinkInfoOf(emptyList(), listOf(ip("::")), emptyList(), 0)
        assertThat(none.gateway).isNull()
    }

    @Test
    fun zoneIdIsStripped() {
        val scoped = InetAddress.getByName("fe80::2%1")
        val info = rawLinkInfoOf(listOf(scoped to 64), emptyList(), emptyList(), 0)
        assertThat(info.ipv6.single().address).doesNotContain("%")
    }
}
