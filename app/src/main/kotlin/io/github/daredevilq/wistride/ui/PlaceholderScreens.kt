package io.github.daredevilq.wistride.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import io.github.daredevilq.wistride.R
import kotlinx.serialization.Serializable

@Serializable
data object SettingsRoute

@Serializable
data object AboutRoute

// Placeholders until the real Settings/About screens land in M8.
internal fun NavGraphBuilder.placeholderScreens() {
    composable<SettingsRoute> { PlaceholderBody(R.string.menu_settings) }
    composable<AboutRoute> { PlaceholderBody(R.string.menu_about) }
}

@Composable
private fun PlaceholderBody(
    @StringRes titleRes: Int,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = stringResource(titleRes), style = MaterialTheme.typography.headlineSmall)
    }
}
