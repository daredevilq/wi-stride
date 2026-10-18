package io.github.daredevilq.wistride.core.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.InputStream
import java.io.OutputStream

/** JSON [Serializer] for [PingSettingsDto]; DataStore already runs it off the main thread. */
object PingSettingsSerializer : Serializer<PingSettingsDto> {
    // Unknown keys: a file written by a newer app version must still be readable after a downgrade.
    private val json = Json { ignoreUnknownKeys = true }

    override val defaultValue: PingSettingsDto = PingSettingsDto.DEFAULT

    @OptIn(ExperimentalSerializationApi::class) // stream (de)serialization is still experimental.
    override suspend fun readFrom(input: InputStream): PingSettingsDto =
        try {
            json.decodeFromStream<PingSettingsDto>(input)
        } catch (e: SerializationException) {
            throw CorruptionException("Cannot read ping settings", e)
        } catch (e: IllegalArgumentException) {
            throw CorruptionException("Cannot read ping settings", e)
        }

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun writeTo(
        t: PingSettingsDto,
        output: OutputStream,
    ) {
        json.encodeToStream(t, output)
    }
}
