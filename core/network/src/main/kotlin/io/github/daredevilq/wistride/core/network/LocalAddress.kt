package io.github.daredevilq.wistride.core.network

import java.net.InetAddress

private const val BITS_PER_BYTE = 8
private const val FULL_BYTE = 0xFF
private const val MAX_OCTET = 255

private class Cidr(
    literal: String,
    val prefix: Int,
) {
    // A numeric literal never triggers a DNS lookup.
    val base: ByteArray = InetAddress.getByName(literal).address

    fun contains(address: ByteArray): Boolean = sharesPrefix(address, base, prefix)
}

// 10/8, 172.16/12, 192.168/16 (RFC 1918), 169.254/16 (link-local), 100.64/10 (CGNAT, tethering/VPN meshes),
// fe80::/10 (IPv6 link-local), fc00::/7 (IPv6 unique-local).
private val LOCAL_RANGES =
    listOf(
        Cidr("10.0.0.0", 8),
        Cidr("172.16.0.0", 12),
        Cidr("192.168.0.0", 16),
        Cidr("169.254.0.0", 16),
        Cidr("100.64.0.0", 10),
        Cidr("fe80::", 10),
        Cidr("fc00::", 7),
    )

/** Compares the first [prefixLength] bits; addresses of different families never match. */
private fun sharesPrefix(
    a: ByteArray,
    b: ByteArray,
    prefixLength: Int,
): Boolean {
    if (a.size != b.size) return false
    val fullBytes = prefixLength / BITS_PER_BYTE
    val restBits = prefixLength % BITS_PER_BYTE
    val fullMatch = (0 until fullBytes).all { a[it] == b[it] }
    val mask = (FULL_BYTE shl (BITS_PER_BYTE - restBits)) and FULL_BYTE
    val restMatch = restBits == 0 || (a[fullBytes].toInt() and mask) == (b[fullBytes].toInt() and mask)
    return fullMatch && restMatch
}

/**
 * True when [address] is on the local network in the sense of Android 17's `ACCESS_LOCAL_NETWORK`
 * — private/link-local/CGNAT ranges, or inside one of the Wi-Fi link's own subnets
 * ([wifiLinkAddresses]). Deliberately generous: a false positive only asks for the permission, a
 * false negative would show a permission denial as packet loss.
 */
fun isLocalAddress(
    address: InetAddress,
    wifiLinkAddresses: List<LinkPrefix>,
): Boolean {
    val bytes = address.address
    return LOCAL_RANGES.any { it.contains(bytes) } ||
        wifiLinkAddresses.any { sharesPrefix(bytes, it.address.address, it.prefixLength) }
}

private val IPV4_LITERAL = Regex("""^(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})$""")

/**
 * Parses [host] if it is an IPv4/IPv6 literal, without any DNS lookup; null for host names.
 * `InetAddress.getByName` does not query DNS for a literal, and a malformed candidate containing a
 * colon throws instead of being resolved.
 */
fun parseIpLiteral(host: String): InetAddress? {
    val v4 = IPV4_LITERAL.matchEntire(host)
    val isCandidate =
        if (v4 != null) v4.groupValues.drop(1).all { it.toInt() <= MAX_OCTET } else ':' in host
    return if (isCandidate) runCatching { InetAddress.getByName(host) }.getOrNull() else null
}
