package io.github.daredevilq.wistride.feature.now

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object NowRoute

fun NavGraphBuilder.nowScreen() {
    composable<NowRoute> { NowScreen() }
}
