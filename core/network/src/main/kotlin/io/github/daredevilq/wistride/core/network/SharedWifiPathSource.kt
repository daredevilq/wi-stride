package io.github.daredevilq.wistride.core.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn

/**
 * Lets every ping target observe the Wi-Fi path through a single [upstream] subscription. Without
 * it each `monitor()` collection would register its own pair of network callbacks, and
 * ConnectivityManager caps the registrations per UID (`TooManyRequestsException`). The upstream
 * runs only while somebody collects, so nothing stays registered when the screen is not visible;
 * the replay cache is dropped when it stops so a restart never starts from a stale network.
 */
internal class SharedWifiPathSource(
    upstream: Flow<WifiPath?>,
    scope: CoroutineScope,
) : WifiPathSource {
    override val path: SharedFlow<WifiPath?> =
        upstream.shareIn(
            scope,
            SharingStarted.WhileSubscribed(replayExpirationMillis = 0),
            replay = 1,
        )
}
