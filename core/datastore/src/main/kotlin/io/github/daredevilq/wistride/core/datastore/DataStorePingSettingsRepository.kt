package io.github.daredevilq.wistride.core.datastore

import androidx.datastore.core.DataStore
import io.github.daredevilq.wistride.core.model.ping.PingSettings
import io.github.daredevilq.wistride.core.model.ping.PingTarget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.util.UUID
import javax.inject.Inject

/** [PingSettingsRepository] backed by a typed DataStore. */
class DataStorePingSettingsRepository
    @Inject
    constructor(
        private val store: DataStore<PingSettingsDto>,
    ) : PingSettingsRepository {
        // A failed read (I/O error) must not kill the screen's flow: fall back to the defaults, like a corrupt file.
        override val settings: Flow<PingSettings> =
            store.data
                .catch { if (it is IOException) emit(PingSettingsDto.DEFAULT) else throw it }
                .map { it.toDomain() }

        override suspend fun addTarget(
            host: String,
            port: Int?,
        ): PingTarget.Host {
            var result: PingTargetDto.Host? = null
            store.updateData { current ->
                val existing =
                    current.targets.filterIsInstance<PingTargetDto.Host>().firstOrNull {
                        it.host.equals(host, ignoreCase = true) && it.port == port
                    }
                if (existing != null) {
                    result = existing
                    current
                } else {
                    require(current.targets.size < PingSettings.MAX_TARGETS) {
                        "At most ${PingSettings.MAX_TARGETS} targets are allowed"
                    }
                    val created = PingTargetDto.Host(id = "user:${UUID.randomUUID()}", host = host, port = port)
                    result = created
                    current.copy(targets = current.targets + created)
                }
            }
            return checkNotNull(result).let { PingTarget.Host(it.id, it.host, it.port) }
        }

        override suspend fun removeTarget(id: String) {
            store.updateData { current ->
                current.copy(targets = current.targets.filterNot { it.stableId() == id })
            }
        }

        override suspend fun setInterval(intervalMs: Long) {
            require(intervalMs in PingSettings.ALLOWED_INTERVALS_MS) { "Interval $intervalMs ms is not allowed" }
            store.updateData { it.copy(intervalMs = intervalMs) }
        }

        override suspend fun restoreDefaultTargets() {
            store.updateData { it.copy(targets = PingSettingsDto.DEFAULT.targets) }
        }
    }

private fun PingTargetDto.stableId(): String =
    when (this) {
        PingTargetDto.Gateway -> PingTarget.Gateway.id
        is PingTargetDto.Host -> id
    }
