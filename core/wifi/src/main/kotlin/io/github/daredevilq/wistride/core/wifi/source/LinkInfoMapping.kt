package io.github.daredevilq.wistride.core.wifi.source

import android.net.LinkProperties
import io.github.daredevilq.wistride.core.model.wifi.IpAddressWithPrefix
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress

internal fun LinkProperties.toRawLinkInfo(): RawLinkInfo =
    rawLinkInfoOf(
        addresses = linkAddresses.map { it.address to it.prefixLength },
        defaultRouteGateways = routes.filter { it.isDefaultRoute }.mapNotNull { it.gateway },
        dnsServers = dnsServers,
        mtu = mtu,
    )

/**
 * Pure part of the `LinkProperties` conversion. The gateway is the next hop of a default route,
 * IPv4 preferred; the unspecified address (`0.0.0.0`/`::`, an on-link route) is not a gateway.
 */
internal fun rawLinkInfoOf(
    addresses: List<Pair<InetAddress, Int>>,
    defaultRouteGateways: List<InetAddress>,
    dnsServers: List<InetAddress>,
    mtu: Int,
): RawLinkInfo {
    val gateways = defaultRouteGateways.filterNot { it.isAnyLocalAddress }
    val gateway = gateways.firstOrNull { it is Inet4Address } ?: gateways.firstOrNull()
    return RawLinkInfo(
        ipv4 = addresses.filter { it.first is Inet4Address }.map { it.toAddressWithPrefix() },
        ipv6 = addresses.filter { it.first is Inet6Address }.map { it.toAddressWithPrefix() },
        gateway = gateway?.textual(),
        dnsServers = dnsServers.map { it.textual() },
        mtu = mtu,
    )
}

private fun Pair<InetAddress, Int>.toAddressWithPrefix() = IpAddressWithPrefix(first.textual(), second)

/** Address text without the `%wlan0` zone suffix that link-local IPv6 addresses carry. */
private fun InetAddress.textual(): String = hostAddress.orEmpty().substringBefore('%')
