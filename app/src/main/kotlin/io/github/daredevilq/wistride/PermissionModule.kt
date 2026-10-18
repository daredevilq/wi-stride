package io.github.daredevilq.wistride

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.daredevilq.wistride.core.model.permission.InMemoryPermissionRequestHistory
import io.github.daredevilq.wistride.core.model.permission.PermissionRequestHistory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PermissionModule {
    /**
     * In-memory for now: after a process restart "don't ask again" is indistinguishable from "never
     * asked". M2 replaces this with the DataStore-backed implementation.
     */
    @Provides
    @Singleton
    fun providesPermissionRequestHistory(): PermissionRequestHistory = InMemoryPermissionRequestHistory()
}
