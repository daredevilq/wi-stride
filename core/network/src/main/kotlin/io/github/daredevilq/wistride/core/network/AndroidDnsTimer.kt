package io.github.daredevilq.wistride.core.network

import android.content.Context
import android.net.DnsResolver
import android.os.Build
import android.os.CancellationSignal
import android.os.Looper
import androidx.annotation.RequiresApi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.net.InetAddress
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

private const val DNS_TIMEOUT_MS = 5_000L
private const val NANOS_PER_MS = 1_000_000.0
private const val RESOLVER_CONSTRUCTOR_API = 37

/**
 * Times `DnsResolver.query` with `FLAG_NO_CACHE_LOOKUP` so the figure is the real round trip to the
 * DNS server, not a cache hit. The overload without a record type asks for A and AAAA together,
 * like `getaddrinfo`.
 *
 * On API 37+ the answers are delivered through the main looper's MessageQueue (the constructor
 * takes a Looper), so a busy UI thread can inflate the measured time slightly; the API offers no
 * way around it. One resolver instance is reused for all measurements.
 */
@Singleton
internal class AndroidDnsTimer
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val clock: LatencyClock,
    ) : DnsTimer {
        private val resolver: DnsResolver by lazy { createResolver() }

        override suspend fun measure(
            network: NetworkHandle,
            host: String,
        ): Double? {
            val androidNetwork = (network as? AndroidNetworkHandle)?.network ?: return null
            return withTimeoutOrNull(DNS_TIMEOUT_MS) {
                suspendCancellableCoroutine { continuation ->
                    val signal = CancellationSignal()
                    continuation.invokeOnCancellation { signal.cancel() }
                    val begin = clock.monotonicNanos()
                    val callback =
                        object : DnsResolver.Callback<List<InetAddress>> {
                            override fun onAnswer(
                                answer: List<InetAddress>,
                                rcode: Int,
                            ) = continuation.resume((clock.monotonicNanos() - begin) / NANOS_PER_MS)

                            override fun onError(error: DnsResolver.DnsException) = continuation.resume(null)
                        }
                    // Runnable::run: the callback only resumes a continuation, no need for another thread hop.
                    resolver.query(
                        androidNetwork,
                        host,
                        DnsResolver.FLAG_NO_CACHE_LOOKUP,
                        Runnable::run,
                        signal,
                        callback,
                    )
                }
            }
        }

        private fun createResolver(): DnsResolver =
            if (Build.VERSION.SDK_INT >= RESOLVER_CONSTRUCTOR_API) {
                newResolver()
            } else {
                // getInstance() is only deprecated from API 37, where the constructor above replaces it.
                @Suppress("DEPRECATION")
                DnsResolver.getInstance()
            }

        @RequiresApi(RESOLVER_CONSTRUCTOR_API)
        private fun newResolver(): DnsResolver = DnsResolver(context, Looper.getMainLooper())
    }
