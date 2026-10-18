package io.github.daredevilq.wistride.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.core.okio.OkioSerializer
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import kotlinx.coroutines.CoroutineScope
import okio.BufferedSink
import okio.BufferedSource
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import okio.buffer
import java.io.File

// The default java.io storage replaces files with File.renameTo, which fails on Windows when the
// target exists (every second write). Okio storage uses an atomic move that works on all hosts, so
// tests run the real serializer and corruption handling on top of it.
private object OkioPingSettingsSerializer : OkioSerializer<PingSettingsDto> {
    override val defaultValue: PingSettingsDto = PingSettingsSerializer.defaultValue

    override suspend fun readFrom(source: BufferedSource): PingSettingsDto =
        PingSettingsSerializer.readFrom(source.inputStream())

    override suspend fun writeTo(
        t: PingSettingsDto,
        sink: BufferedSink,
    ) {
        PingSettingsSerializer.writeTo(t, sink.outputStream())
        sink.flush()
    }
}

internal fun pingSettingsStore(
    file: File,
    scope: CoroutineScope,
): DataStore<PingSettingsDto> =
    DataStoreFactory.create(
        storage = OkioStorage(FileSystem.SYSTEM, OkioPingSettingsSerializer) { file.toOkioPath() },
        corruptionHandler = ReplaceFileCorruptionHandler { PingSettingsDto.DEFAULT },
        scope = scope,
    )

internal fun permissionRequestsStore(
    file: File,
    scope: CoroutineScope,
): DataStore<Preferences> =
    PreferenceDataStoreFactory.create(
        storage = OkioStorage(FileSystem.SYSTEM, PreferencesSerializer) { file.toOkioPath() },
        scope = scope,
    )
