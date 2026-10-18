package io.github.daredevilq.wistride.core.wifi.di

import android.content.Context
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Build
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.daredevilq.wistride.core.wifi.DefaultWifiConnectionRepository
import io.github.daredevilq.wistride.core.wifi.WifiConnectionRepository
import io.github.daredevilq.wistride.core.wifi.source.AndroidConnectivitySource
import io.github.daredevilq.wistride.core.wifi.source.AndroidScanResultsSource
import io.github.daredevilq.wistride.core.wifi.source.AndroidWifiPermissionChecker
import io.github.daredevilq.wistride.core.wifi.source.AndroidWifiStateSource
import io.github.daredevilq.wistride.core.wifi.source.ConnectivitySource
import io.github.daredevilq.wistride.core.wifi.source.ScanResultsSource
import io.github.daredevilq.wistride.core.wifi.source.WifiPermissionChecker
import io.github.daredevilq.wistride.core.wifi.source.WifiStateSource
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class WifiBindingsModule {
    @Binds
    abstract fun bindRepository(impl: DefaultWifiConnectionRepository): WifiConnectionRepository

    @Binds
    abstract fun bindStateSource(impl: AndroidWifiStateSource): WifiStateSource

    @Binds
    abstract fun bindConnectivitySource(impl: AndroidConnectivitySource): ConnectivitySource

    @Binds
    abstract fun bindScanResultsSource(impl: AndroidScanResultsSource): ScanResultsSource

    @Binds
    abstract fun bindPermissionChecker(impl: AndroidWifiPermissionChecker): WifiPermissionChecker
}

@Module
@InstallIn(SingletonComponent::class)
internal object WifiProvidersModule {
    @Provides
    @Singleton
    fun provideWifiManager(
        @ApplicationContext context: Context,
    ): WifiManager = context.getSystemService(WifiManager::class.java)

    @Provides
    @Singleton
    fun provideConnectivityManager(
        @ApplicationContext context: Context,
    ): ConnectivityManager = context.getSystemService(ConnectivityManager::class.java)

    @Provides
    @Singleton
    fun provideLocationManager(
        @ApplicationContext context: Context,
    ): LocationManager = context.getSystemService(LocationManager::class.java)

    @Provides
    @SdkInt
    fun provideSdkInt(): Int = Build.VERSION.SDK_INT
}
