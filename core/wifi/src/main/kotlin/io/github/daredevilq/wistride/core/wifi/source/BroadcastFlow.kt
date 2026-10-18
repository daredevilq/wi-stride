package io.github.daredevilq.wistride.core.wifi.source

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Cold flow of [read] evaluated once on collection (intent = null) and after each broadcast of
 * [actions], in the collector's context rather than on the receiver (main) thread. System
 * broadcasts are still delivered to a NOT_EXPORTED receiver. The receiver is unregistered when the
 * collector goes away.
 */
internal fun <T> Context.broadcastFlow(
    vararg actions: String,
    read: (Intent?) -> T,
): Flow<T> =
    callbackFlow<Intent?> {
        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(
                    context: Context,
                    intent: Intent,
                ) {
                    // Only forward the trigger: `read` makes binder calls and must not run on the main thread.
                    trySend(intent)
                }
            }
        val filter = IntentFilter().apply { actions.forEach(::addAction) }
        ContextCompat.registerReceiver(this@broadcastFlow, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        trySend(null)
        awaitClose { unregisterReceiver(receiver) }
    }.conflate().map(read).distinctUntilChanged()
