package io.github.daredevilq.wistride.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.daredevilq.wistride.R
import io.github.daredevilq.wistride.feature.map.mapScreen
import io.github.daredevilq.wistride.feature.networks.networksScreen
import io.github.daredevilq.wistride.feature.now.NowRoute
import io.github.daredevilq.wistride.feature.now.nowScreen
import io.github.daredevilq.wistride.feature.ping.pingScreen
import io.github.daredevilq.wistride.feature.speed.speedScreen

/** App shell: adaptive navigation (bar on phones, rail on large screens) plus a top app bar. */
@Composable
fun WiStrideApp(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val current = destination.toTopLevelDestination()
    val titleRes = titleFor(current, destination)
    val suiteType =
        if (current == null) {
            NavigationSuiteType.None
        } else {
            NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(currentWindowAdaptiveInfoV2())
        }

    NavigationSuiteScaffold(
        modifier = modifier,
        layoutType = suiteType,
        navigationSuiteItems = {
            TopLevelDestination.entries.forEach { tab ->
                item(
                    selected = tab == current,
                    onClick = { navController.navigateToTab(tab) },
                    icon = { Icon(painterResource(tab.icon), contentDescription = null) },
                    label = { Text(stringResource(tab.label)) },
                )
            }
        },
    ) {
        Scaffold(
            topBar = {
                WiStrideTopBar(
                    titleRes = titleRes,
                    isTopLevel = current != null,
                    onBack = navController::navigateUp,
                    onSettings = { navController.navigate(SettingsRoute) },
                    onAbout = { navController.navigate(AboutRoute) },
                )
            },
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = NowRoute,
                modifier = Modifier.padding(padding),
            ) {
                nowScreen()
                pingScreen()
                networksScreen()
                speedScreen()
                mapScreen()
                placeholderScreens()
            }
        }
    }
}

/**
 * Tab owning [this] destination. The back stack is empty on the first frame (launch, recreation);
 * the start tab is returned then so the shell never flashes a back arrow without the navigation bar.
 */
private fun NavDestination?.toTopLevelDestination(): TopLevelDestination? =
    if (this == null) {
        TopLevelDestination.NOW
    } else {
        TopLevelDestination.entries.firstOrNull { tab -> hierarchy.any { it.hasRoute(tab.route::class) } }
    }

@StringRes
private fun titleFor(
    current: TopLevelDestination?,
    destination: NavDestination?,
): Int =
    when {
        current != null -> current.label
        destination?.hasRoute(SettingsRoute::class) == true -> R.string.menu_settings
        destination?.hasRoute(AboutRoute::class) == true -> R.string.menu_about
        else -> R.string.app_name
    }

private fun NavHostController.navigateToTab(tab: TopLevelDestination) {
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WiStrideTopBar(
    @StringRes titleRes: Int,
    isTopLevel: Boolean,
    onBack: () -> Unit,
    onSettings: () -> Unit,
    onAbout: () -> Unit,
) {
    TopAppBar(
        title = { Text(stringResource(titleRes)) },
        navigationIcon = {
            if (!isTopLevel) {
                IconButton(onClick = onBack) {
                    Icon(
                        painterResource(R.drawable.ic_arrow_back),
                        contentDescription = stringResource(R.string.action_navigate_up),
                    )
                }
            }
        },
        actions = {
            if (isTopLevel) OverflowMenu(onSettings = onSettings, onAbout = onAbout)
        },
    )
}

@Composable
private fun OverflowMenu(
    onSettings: () -> Unit,
    onAbout: () -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }) {
        Icon(
            painterResource(R.drawable.ic_more_vert),
            contentDescription = stringResource(R.string.action_more_options),
        )
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_settings)) },
            onClick = {
                expanded = false
                onSettings()
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_about)) },
            onClick = {
                expanded = false
                onAbout()
            },
        )
    }
}
