package io.github.daredevilq.wistride.core.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn

/**
 * Keeps the callbacks registered briefly after the last collector leaves: the Ping screen restarts
 * all targets at once (interval or permission change), which would otherwise unregister and
 * re-register them in the same instant.
 */
private const val RESTART_GRACE_MS = 1_000L

/**
 * Lets every ping target observe the Wi-Fi path through a single [upstream] subscription. Without
 * it each `monitor()` collection would register its own pair of network callbacks, and
 * ConnectivityManager caps the registrations per UID (`TooManyRequestsException`). The upstream
 * runs only while somebody collects (plus [RESTART_GRACE_MS]), so nothing stays registered when
 * the screen is not visible; the replay cache is dropped when it stops so a restart never starts
 * from a stale network.
 */
internal class SharedWifiPathSource(
    upstream: Flow<WifiPath?>,
    scope: CoroutineScope,
) : WifiPathSource {
    override val path: SharedFlow<WifiPath?> =
        upstream.shareIn(
            scope,
            SharingStarted.WhileSubscribed(stopTimeoutMillis = RESTART_GRACE_MS, replayExpirationMillis = 0),
            replay = 1,
        )
}
