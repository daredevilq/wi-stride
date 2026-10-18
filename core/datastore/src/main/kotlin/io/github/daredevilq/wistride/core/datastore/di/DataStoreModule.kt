package io.github.daredevilq.wistride.core.datastore.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.daredevilq.wistride.core.common.Dispatcher
import io.github.daredevilq.wistride.core.common.WiStrideDispatcher
import io.github.daredevilq.wistride.core.datastore.DataStorePermissionRequestHistory
import io.github.daredevilq.wistride.core.datastore.DataStorePingSettingsRepository
import io.github.daredevilq.wistride.core.datastore.PingSettingsDto
import io.github.daredevilq.wistride.core.datastore.PingSettingsRepository
import io.github.daredevilq.wistride.core.datastore.PingSettingsSerializer
import io.github.daredevilq.wistride.core.model.permission.PermissionRequestHistory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

private const val PING_SETTINGS_FILE = "ping_settings.json"
private const val PERMISSION_REQUESTS_FILE = "permission_requests"

/**
 * Provides the two app-wide DataStores. Separate from [DataStoreBindingsModule] so Hilt tests can
 * replace the bindings (`TestDataStoreModule` takes the place of [DataStoreBindingsModule]) with
 * in-memory implementations: DataStore allows one active instance per file per process, and Hilt
 * tests build a new singleton component per test.
 */
@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    @Provides
    @Singleton
    fun providesPingSettingsStore(
        @ApplicationContext context: Context,
        @Dispatcher(WiStrideDispatcher.IO) ioDispatcher: CoroutineDispatcher,
    ): DataStore<PingSettingsDto> =
        DataStoreFactory.create(
            serializer = PingSettingsSerializer,
            corruptionHandler = ReplaceFileCorruptionHandler { PingSettingsDto.DEFAULT },
            scope = CoroutineScope(ioDispatcher + SupervisorJob()),
            produceFile = { context.dataStoreFile(PING_SETTINGS_FILE) },
        )

    @Provides
    @Singleton
    fun providesPermissionRequestsStore(
        @ApplicationContext context: Context,
        @Dispatcher(WiStrideDispatcher.IO) ioDispatcher: CoroutineDispatcher,
    ): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            scope = CoroutineScope(ioDispatcher + SupervisorJob()),
            produceFile = { context.preferencesDataStoreFile(PERMISSION_REQUESTS_FILE) },
        )
}

/** Binds the DataStore-backed implementations to the interfaces the features inject. */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataStoreBindingsModule {
    @Binds
    @Singleton
    abstract fun bindsPingSettingsRepository(impl: DataStorePingSettingsRepository): PingSettingsRepository

    @Binds
    @Singleton
    abstract fun bindsPermissionRequestHistory(impl: DataStorePermissionRequestHistory): PermissionRequestHistory
}
