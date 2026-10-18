package io.github.daredevilq.wistride.core.network

import android.net.Network
import java.net.InetAddress
import java.net.InetSocketAddress

/** [NetworkHandle] over a real `android.net.Network`; all sockets and lookups are bound to it. */
internal data class AndroidNetworkHandle(
    val network: Network,
) : NetworkHandle {
    override fun resolve(host: String): List<InetAddress> = network.getAllByName(host).toList()

    override fun connect(
        address: InetAddress,
        port: Int,
        timeoutMs: Int,
    ) {
        network.socketFactory.createSocket().use { it.connect(InetSocketAddress(address, port), timeoutMs) }
    }
}
