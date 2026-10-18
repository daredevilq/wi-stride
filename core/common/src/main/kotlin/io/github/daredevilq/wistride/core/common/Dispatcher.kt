package io.github.daredevilq.wistride.core.common

import javax.inject.Qualifier

/** Qualifies an injected [kotlinx.coroutines.CoroutineDispatcher] so tests can swap it for a test dispatcher. */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class Dispatcher(
    val dispatcher: WiStrideDispatcher,
)

enum class WiStrideDispatcher {
    /** CPU-bound work: sensor processing, PDR, interpolation. */
    Default,

    /** Blocking I/O: database, files, sockets, processes. */
    IO,
}
