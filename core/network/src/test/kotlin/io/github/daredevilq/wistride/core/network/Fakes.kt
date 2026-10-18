package io.github.daredevilq.wistride.core.network

import io.github.daredevilq.wistride.core.model.ping.LatencyEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.test.TestCoroutineScheduler
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

fun ip(literal: String): InetAddress = InetAddress.getByName(literal)

/** Monotonic time = virtual test time + [extraNanos] that fakes add to simulate work. */
@OptIn(ExperimentalCoroutinesApi::class)
class FakeClock(
    private val scheduler: TestCoroutineScheduler,
) : LatencyClock {
    var extraNanos = 0L

    override fun wallClockMs(): Long = 1_000_000L + scheduler.currentTime

    override fun monotonicNanos(): Long = scheduler.currentTime * 1_000_000L + extraNanos
}

class FakeNetworkHandle(
    val name: String = "wifi",
    var onResolve: (String) -> List<InetAddress> = { throw UnknownHostException(it) },
    var onConnect: (InetAddress, Int, Int) -> Unit = { _, _, _ -> },
) : NetworkHandle {
    val connects = mutableListOf<Triple<InetAddress, Int, Int>>()
    var onOpen: () -> Unit = {}
    var opened = 0
    var closed = 0

    override fun resolve(host: String): List<InetAddress> = onResolve(host)

    override fun openConnection(): TcpConnection {
        onOpen()
        opened++
        return object : TcpConnection {
            override fun connect(
                address: InetAddress,
                port: Int,
                timeoutMs: Int,
            ) {
                connects += Triple(address, port, timeoutMs)
                onConnect(address, port, timeoutMs)
            }

            override fun close() {
                closed++
            }
        }
    }
}

class FakeProcess : RunningProcess {
    private val eof = Any()
    private val queue = LinkedBlockingQueue<Any>()
    val destroyed = AtomicBoolean(false)
    val forced = AtomicBoolean(false)

    fun emitLine(line: String) = queue.put(line)

    fun end() = queue.put(eof)

    override fun readLine(): String? {
        val next = queue.take()
        if (next === eof) {
            queue.put(eof)
            return null
        }
        return next as String
    }

    override fun destroy() {
        destroyed.set(true)
        end()
    }

    override fun destroyForcibly() = forced.set(true)

    override fun waitForExit(timeoutMs: Long): Boolean = destroyed.get()
}

class FakePathSource(
    initial: WifiPath?,
) : WifiPathSource {
    val state = MutableStateFlow(initial)
    override val path: Flow<WifiPath?> = state
}

class FakeIcmp(
    var behaviour: () -> Flow<LatencyEvent.Probe> = { flow { awaitCancellation() } },
) : IcmpProber {
    val calls = AtomicInteger()
    val active = AtomicInteger()

    override fun probe(
        address: InetAddress,
        intervalMs: Long,
    ): Flow<LatencyEvent.Probe> {
        calls.incrementAndGet()
        active.incrementAndGet()
        return behaviour().onCompletion { active.decrementAndGet() }
    }
}

class FakeTcp : TcpProber {
    val calls = mutableListOf<Int>()
    val active = AtomicInteger()

    override fun probe(
        network: NetworkHandle,
        address: InetAddress,
        port: Int,
        intervalMs: Long,
    ): Flow<LatencyEvent.Probe> {
        calls += port
        active.incrementAndGet()
        return flow {
            emit(LatencyEvent.Probe(TCP_TIMESTAMP, TCP_RTT))
            awaitCancellation()
        }.onCompletion { active.decrementAndGet() }
    }

    companion object {
        const val TCP_TIMESTAMP = 111L
        const val TCP_RTT = 7.5
    }
}

class FakeDns : DnsTimer {
    val hosts = mutableListOf<String>()

    override suspend fun measure(
        network: NetworkHandle,
        host: String,
    ): Double? {
        hosts += host
        return DNS_MS
    }

    companion object {
        const val DNS_MS = 23.0
    }
}
