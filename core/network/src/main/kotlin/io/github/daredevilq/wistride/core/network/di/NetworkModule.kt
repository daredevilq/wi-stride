package io.github.daredevilq.wistride.core.network.di

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.daredevilq.wistride.core.common.Dispatcher
import io.github.daredevilq.wistride.core.common.WiStrideDispatcher
import io.github.daredevilq.wistride.core.network.AndroidDnsTimer
import io.github.daredevilq.wistride.core.network.AndroidWifiPathSource
import io.github.daredevilq.wistride.core.network.ConnectTcpProber
import io.github.daredevilq.wistride.core.network.DefaultLatencyMonitor
import io.github.daredevilq.wistride.core.network.DnsTimer
import io.github.daredevilq.wistride.core.network.IcmpProber
import io.github.daredevilq.wistride.core.network.LatencyClock
import io.github.daredevilq.wistride.core.network.LatencyMonitor
import io.github.daredevilq.wistride.core.network.LocalNetworkPermissionChecker
import io.github.daredevilq.wistride.core.network.ProcessIcmpProber
import io.github.daredevilq.wistride.core.network.ProcessRunner
import io.github.daredevilq.wistride.core.network.SharedWifiPathSource
import io.github.daredevilq.wistride.core.network.SystemProcessRunner
import io.github.daredevilq.wistride.core.network.TcpProber
import io.github.daredevilq.wistride.core.network.WifiPathSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class NetworkBindingsModule {
    @Binds
    abstract fun bindMonitor(impl: DefaultLatencyMonitor): LatencyMonitor

    @Binds
    abstract fun bindIcmpProber(impl: ProcessIcmpProber): IcmpProber

    @Binds
    abstract fun bindTcpProber(impl: ConnectTcpProber): TcpProber

    @Binds
    abstract fun bindDnsTimer(impl: AndroidDnsTimer): DnsTimer

    @Binds
    abstract fun bindProcessRunner(impl: SystemProcessRunner): ProcessRunner
}

@Module
@InstallIn(SingletonComponent::class)
internal object NetworkProvidersModule {
    @Provides
    @SdkInt
    fun provideSdkInt(): Int = Build.VERSION.SDK_INT

    @Provides
    @Singleton
    @NetworkScope
    fun provideNetworkScope(
        @Dispatcher(WiStrideDispatcher.IO) io: CoroutineDispatcher,
    ): CoroutineScope = CoroutineScope(SupervisorJob() + io)

    @Provides
    @Singleton
    fun providePathSource(
        platform: AndroidWifiPathSource,
        @NetworkScope scope: CoroutineScope,
    ): WifiPathSource = SharedWifiPathSource(platform.path, scope)

    @Provides
    @Singleton
    fun provideClock(): LatencyClock =
        object : LatencyClock {
            override fun wallClockMs(): Long = System.currentTimeMillis()

            override fun monotonicNanos(): Long = System.nanoTime()
        }

    @Provides
    fun provideLocalNetworkPermissionChecker(
        @ApplicationContext context: Context,
    ): LocalNetworkPermissionChecker =
        LocalNetworkPermissionChecker {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_LOCAL_NETWORK) ==
                PackageManager.PERMISSION_GRANTED
        }
}
