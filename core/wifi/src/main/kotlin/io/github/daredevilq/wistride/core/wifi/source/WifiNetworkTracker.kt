package io.github.daredevilq.wistride.core.wifi.source

/**
 * Bookkeeping for several simultaneous Wi-Fi networks (e.g. STA+STA). Android-free so it can be
 * unit-tested; [K] is the platform `Network`. `WifiInfo.isPrimary` is not public API, so the most
 * recently available network wins. Callbacks and the poller run on different threads, hence the locks.
 */
internal class WifiNetworkTracker<K : Any> {
    private class Entry(
        val order: Long,
        var info: RawWifiInfo? = null,
        var link: RawLinkInfo? = null,
    )

    private val entries = LinkedHashMap<K, Entry>()
    private var counter = 0L

    /** API 29-30 only: `WifiManager.connectionInfo`, which is not tied to a `Network`. */
    private var polledInfo: RawWifiInfo? = null

    @Synchronized
    fun onAvailable(key: K) {
        if (entries.isEmpty()) polledInfo = null
        entries[key] = Entry(order = counter++)
    }

    @Synchronized
    fun onWifiInfo(
        key: K,
        info: RawWifiInfo?,
    ) {
        entry(key).info = info
    }

    @Synchronized
    fun onLinkInfo(
        key: K,
        link: RawLinkInfo,
    ) {
        entry(key).link = link
    }

    @Synchronized
    fun onLost(key: K) {
        entries.remove(key)
        if (entries.isEmpty()) polledInfo = null
    }

    @Synchronized
    fun setPolledInfo(info: RawWifiInfo?) {
        // A poll in flight when onLost fired must not resurrect stale info.
        if (entries.isEmpty()) return
        polledInfo = info
    }

    @Synchronized
    fun isEmpty(): Boolean = entries.isEmpty()

    @Synchronized
    fun current(): WifiNetworkInfo? {
        val selected = entries.values.maxByOrNull { it.order } ?: return null
        return WifiNetworkInfo(wifiInfo = selected.info ?: polledInfo, link = selected.link)
    }

    private fun entry(key: K): Entry = entries.getOrPut(key) { Entry(order = counter++) }
}
