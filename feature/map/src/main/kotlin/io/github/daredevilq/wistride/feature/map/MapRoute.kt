package io.github.daredevilq.wistride.feature.map

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object MapRoute

fun NavGraphBuilder.mapScreen() {
    composable<MapRoute> { MapScreen() }
}
