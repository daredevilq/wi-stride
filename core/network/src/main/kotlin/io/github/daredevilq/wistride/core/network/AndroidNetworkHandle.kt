package io.github.daredevilq.wistride.core.network

import android.net.Network
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

/** [NetworkHandle] over a real `android.net.Network`; all sockets and lookups are bound to it. */
internal data class AndroidNetworkHandle(
    val network: Network,
) : NetworkHandle {
    override fun resolve(host: String): List<InetAddress> = network.getAllByName(host).toList()

    override fun openConnection(): TcpConnection = AndroidTcpConnection(network.socketFactory.createSocket())
}

private class AndroidTcpConnection(
    private val socket: Socket,
) : TcpConnection {
    override fun connect(
        address: InetAddress,
        port: Int,
        timeoutMs: Int,
    ) = socket.connect(InetSocketAddress(address, port), timeoutMs)

    override fun close() {
        try {
            socket.close()
        } catch (_: IOException) {
            // Nothing to recover: the probe result is already known.
        }
    }
}
