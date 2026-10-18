package io.github.daredevilq.wistride.feature.speed

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object SpeedRoute

fun NavGraphBuilder.speedScreen() {
    composable<SpeedRoute> { SpeedScreen() }
}
