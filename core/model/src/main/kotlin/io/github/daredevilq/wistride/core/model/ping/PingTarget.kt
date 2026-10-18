package io.github.daredevilq.wistride.core.model.ping

/** Something the Ping screen measures latency to. */
sealed interface PingTarget {
    /** Stable key: identifies the target across restarts and list changes. */
    val id: String

    /** The default gateway of the current Wi-Fi network, looked up from `LinkProperties` when measuring. */
    data object Gateway : PingTarget {
        override val id: String = "gateway"
    }

    /**
     * A host name or IP literal. With a [port] only a TCP connect to that port is measured; without
     * one the best available method is used.
     */
    data class Host(
        override val id: String,
        val host: String,
        val port: Int? = null,
    ) : PingTarget
}

/** User-editable Ping screen configuration, persisted in `core:datastore`. */
data class PingSettings(
    val targets: List<PingTarget>,
    val intervalMs: Long,
) {
    companion object {
        const val DEFAULT_INTERVAL_MS: Long = 1_000L

        /**
         * Upper bound of the target list. Every target owns a `ping` process or a TCP prober, so this
         * bounds the processes and the work per probe tick.
         */
        const val MAX_TARGETS: Int = 20

        /**
         * Offered probe intervals. 500 ms is the floor: the system `ping` refuses intervals below 200 ms
         * for non-root users, and faster probing adds load without making the statistics more useful.
         */
        val ALLOWED_INTERVALS_MS: List<Long> = listOf(500L, 1_000L, 2_000L, 5_000L)

        /** Defaults: the gateway plus two public resolvers and three popular host names. */
        val DEFAULT_TARGETS: List<PingTarget> =
            listOf(PingTarget.Gateway) +
                listOf("8.8.8.8", "1.1.1.1", "google.com", "facebook.com", "x.com")
                    .map { PingTarget.Host(id = "default:$it", host = it) }

        val DEFAULT: PingSettings = PingSettings(DEFAULT_TARGETS, DEFAULT_INTERVAL_MS)
    }
}
