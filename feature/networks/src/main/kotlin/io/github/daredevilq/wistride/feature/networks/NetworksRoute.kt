package io.github.daredevilq.wistride.feature.networks

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object NetworksRoute

fun NavGraphBuilder.networksScreen() {
    composable<NetworksRoute> { NetworksScreen() }
}
