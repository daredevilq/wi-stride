package io.github.daredevilq.wistride

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import io.github.daredevilq.wistride.core.datastore.PingSettingsRepository
import io.github.daredevilq.wistride.core.datastore.di.DataStoreBindingsModule
import io.github.daredevilq.wistride.core.model.permission.InMemoryPermissionRequestHistory
import io.github.daredevilq.wistride.core.model.permission.PermissionRequestHistory
import io.github.daredevilq.wistride.core.model.ping.PingSettings
import io.github.daredevilq.wistride.core.model.ping.PingTarget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Singleton

/**
 * Hilt tests build a new singleton component per test, and DataStore refuses two active instances
 * on the same file in one process, so tests bind in-memory stand-ins instead of the real stores.
 */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [DataStoreBindingsModule::class])
object TestDataStoreModule {
    @Provides
    @Singleton
    fun providesPermissionRequestHistory(): PermissionRequestHistory = InMemoryPermissionRequestHistory()

    @Provides
    @Singleton
    fun providesPingSettingsRepository(): PingSettingsRepository = InMemoryPingSettingsRepository()
}

private class InMemoryPingSettingsRepository : PingSettingsRepository {
    private val state = MutableStateFlow(PingSettings.DEFAULT)

    override val settings: Flow<PingSettings> = state

    override suspend fun addTarget(
        host: String,
        port: Int?,
    ): PingTarget.Host {
        val target = PingTarget.Host(id = "user:$host:$port", host = host, port = port)
        state.value = state.value.copy(targets = state.value.targets + target)
        return target
    }

    override suspend fun removeTarget(id: String) {
        state.value = state.value.copy(targets = state.value.targets.filterNot { it.id == id })
    }

    override suspend fun setInterval(intervalMs: Long) {
        state.value = state.value.copy(intervalMs = intervalMs)
    }

    override suspend fun restoreDefaultTargets() {
        state.value = state.value.copy(targets = PingSettings.DEFAULT_TARGETS)
    }
}
