package io.github.daredevilq.wistride.core.datastore

import io.github.daredevilq.wistride.core.model.ping.PingSettings
import io.github.daredevilq.wistride.core.model.ping.PingTarget
import kotlinx.coroutines.flow.Flow

/** Persistent Ping screen configuration. */
interface PingSettingsRepository {
    /** Current settings first, then after every change; [PingSettings.DEFAULT] until the user edits them. */
    val settings: Flow<PingSettings>

    /** Appends a user target; [host] and [port] must already be validated. Returns the stored target. */
    suspend fun addTarget(
        host: String,
        port: Int?,
    ): PingTarget.Host

    /** Removes the target with [id] (defaults included); unknown ids are ignored. */
    suspend fun removeTarget(id: String)

    /** Sets the probe interval; must be one of [PingSettings.ALLOWED_INTERVALS_MS]. */
    suspend fun setInterval(intervalMs: Long)

    /** Replaces the target list with [PingSettings.DEFAULT_TARGETS]; the interval is kept. */
    suspend fun restoreDefaultTargets()
}
