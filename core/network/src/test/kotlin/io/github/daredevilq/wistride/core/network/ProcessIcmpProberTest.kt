package io.github.daredevilq.wistride.core.network

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineScheduler
import org.junit.Test
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Runs on real IO threads: the fake process blocks in `readLine` exactly like the real one. */
class ProcessIcmpProberTest {
    private val process = FakeProcess()
    private var command: List<String> = emptyList()
    private val clock = FakeClock(TestCoroutineScheduler())

    private fun prober(start: (List<String>) -> RunningProcess = { process }) =
        ProcessIcmpProber(
            runner =
                ProcessRunner {
                    command = it
                    start(it)
                },
            clock = clock,
            io = Dispatchers.IO,
        )

    @Test
    fun `maps replies and no-answer lines to probes`() =
        runBlocking {
            prober().probe(ip("8.8.8.8"), 1_000).test {
                process.emitLine("PING 8.8.8.8 (8.8.8.8) 56(84) bytes of data.")
                process.emitLine("64 bytes from 8.8.8.8: icmp_seq=1 ttl=113 time=14.2 ms")
                process.emitLine("[1792314001.676483] no answer yet for icmp_seq=2")
                process.emitLine("64 bytes from 8.8.8.8: icmp_seq=4 ttl=113 time=9.5 ms")
                assertThat(awaitItem().rttMs).isEqualTo(14.2)
                assertThat(awaitItem().rttMs).isNull()
                assertThat(awaitItem().rttMs).isNull()
                assertThat(awaitItem().rttMs).isEqualTo(9.5)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `builds the ping command`() =
        runBlocking {
            prober().probe(ip("8.8.8.8"), 500).test {
                process.emitLine("64 bytes from 8.8.8.8: icmp_seq=1 ttl=113 time=1 ms")
                awaitItem()
                cancelAndIgnoreRemainingEvents()
            }
            assertThat(command).containsExactly("ping", "-n", "-O", "-i", "0.5", "8.8.8.8").inOrder()
        }

    @Test
    fun `uses ping6 for IPv6`() =
        runBlocking {
            prober().probe(ip("2001:4860:4860::8888"), 1_000).test {
                process.end()
                awaitError()
            }
            assertThat(command.first()).isEqualTo("ping6")
            assertThat(command[4]).isEqualTo("1.0")
        }

    @Test
    fun `cancelling the collector destroys the process`() =
        runBlocking {
            prober().probe(ip("8.8.8.8"), 1_000).test {
                process.emitLine("64 bytes from 8.8.8.8: icmp_seq=1 ttl=113 time=1 ms")
                awaitItem()
                cancelAndIgnoreRemainingEvents()
            }
            assertThat(process.destroyed.get()).isTrue()
        }

    @Test
    fun `early exit is reported as unusable`() =
        runBlocking {
            prober().probe(ip("8.8.8.8"), 1_000).test {
                process.emitLine("ping: socket: Operation not permitted")
                process.end()
                assertThat(awaitError()).isInstanceOf(IcmpUnusableException::class.java)
            }
            assertThat(process.destroyed.get()).isTrue()
        }

    @Test
    fun `ping ending before any reply has no replies`() =
        runBlocking {
            prober().probe(ip("8.8.8.8"), 1_000).test {
                process.emitLine("[1792314001.676483] no answer yet for icmp_seq=1")
                awaitItem()
                process.end()
                assertThat((awaitError() as IcmpUnusableException).hadReplies).isFalse()
            }
        }

    @Test
    fun `ping ending after a reply is flagged as having replied`() =
        runBlocking {
            prober().probe(ip("8.8.8.8"), 1_000).test {
                process.emitLine("64 bytes from 8.8.8.8: icmp_seq=1 ttl=113 time=1 ms")
                awaitItem()
                process.end()
                assertThat((awaitError() as IcmpUnusableException).hadReplies).isTrue()
            }
        }

    @Test
    fun `cancelling while ping is still starting destroys the process once it exists`() =
        runBlocking {
            val entered = CountDownLatch(1)
            val release = CountDownLatch(1)
            val prober =
                prober {
                    entered.countDown()
                    release.await(LATCH_TIMEOUT_S, TimeUnit.SECONDS)
                    process
                }
            val job = launch(Dispatchers.Default) { prober.probe(ip("8.8.8.8"), 1_000).collect { } }
            assertThat(entered.await(LATCH_TIMEOUT_S, TimeUnit.SECONDS)).isTrue()
            job.cancel()
            release.countDown()
            job.join()
            assertThat(process.destroyed.get()).isTrue()
        }

    @Test
    fun `exec failure is reported as unusable`() =
        runBlocking {
            prober { throw IOException("no such file") }.probe(ip("8.8.8.8"), 1_000).test {
                assertThat(awaitError()).isInstanceOf(IcmpUnusableException::class.java)
            }
        }

    @Test
    fun `probe timestamps come from the wall clock`() =
        runBlocking {
            prober().probe(ip("8.8.8.8"), 1_000).test {
                process.emitLine("64 bytes from 8.8.8.8: icmp_seq=1 ttl=113 time=1 ms")
                assertThat(awaitItem().timestampMs).isEqualTo(clock.wallClockMs())
                cancelAndIgnoreRemainingEvents()
            }
        }

    private companion object {
        const val LATCH_TIMEOUT_S = 5L
    }
}
