package io.github.daredevilq.wistride.core.datastore

import io.github.daredevilq.wistride.core.model.ping.PingSettings
import io.github.daredevilq.wistride.core.model.ping.PingTarget
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * On-disk shape of [PingSettings], kept apart from the domain model so the file format can evolve
 * independently (bump [schemaVersion] and migrate in [PingSettingsSerializer]).
 *
 * Public only because the Hilt module providing the DataStore must be visible to test modules;
 * nothing outside `core:datastore` should use it.
 */
@Serializable
data class PingSettingsDto(
    val schemaVersion: Int = SCHEMA_VERSION,
    val targets: List<PingTargetDto>,
    val intervalMs: Long,
) {
    companion object {
        const val SCHEMA_VERSION = 1

        val DEFAULT: PingSettingsDto = PingSettings.DEFAULT.toDto()
    }
}

/** On-disk shape of [PingTarget]. */
@Serializable
sealed interface PingTargetDto {
    @Serializable
    @SerialName("gateway")
    data object Gateway : PingTargetDto

    @Serializable
    @SerialName("host")
    data class Host(
        val id: String,
        val host: String,
        val port: Int? = null,
    ) : PingTargetDto
}

internal fun PingSettings.toDto(): PingSettingsDto =
    PingSettingsDto(
        targets =
            targets.map {
                when (it) {
                    PingTarget.Gateway -> PingTargetDto.Gateway
                    is PingTarget.Host -> PingTargetDto.Host(it.id, it.host, it.port)
                }
            },
        intervalMs = intervalMs,
    )

/** An interval outside [PingSettings.ALLOWED_INTERVALS_MS] (older or newer app version) falls back to the default. */
internal fun PingSettingsDto.toDomain(): PingSettings =
    PingSettings(
        targets =
            targets.map {
                when (it) {
                    PingTargetDto.Gateway -> PingTarget.Gateway
                    is PingTargetDto.Host -> PingTarget.Host(it.id, it.host, it.port)
                }
            },
        intervalMs =
            intervalMs.takeIf { it in PingSettings.ALLOWED_INTERVALS_MS } ?: PingSettings.DEFAULT_INTERVAL_MS,
    )
