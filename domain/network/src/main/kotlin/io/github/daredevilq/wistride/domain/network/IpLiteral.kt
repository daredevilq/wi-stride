package io.github.daredevilq.wistride.domain.network

/**
 * Whether an already validated [host] (as stored by [validateTarget]) is an IP literal rather than a
 * host name; only host names involve a DNS lookup.
 */
fun isIpLiteral(host: String): Boolean = ':' in host || isIpv4(host.split('.'))
