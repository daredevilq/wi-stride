package io.github.daredevilq.wistride.feature.ping

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object PingRoute

fun NavGraphBuilder.pingScreen() {
    composable<PingRoute> { PingScreen() }
}
