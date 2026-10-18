package io.github.daredevilq.wistride.core.network.di

import javax.inject.Qualifier

/** The device's `Build.VERSION.SDK_INT`; injected so tests can exercise other API levels. */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class SdkInt
