package io.github.daredevilq.wistride.core.network.di

import javax.inject.Qualifier

/** App-lifetime scope that hosts the shared upstreams of core:network (IO dispatcher, supervised). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class NetworkScope
