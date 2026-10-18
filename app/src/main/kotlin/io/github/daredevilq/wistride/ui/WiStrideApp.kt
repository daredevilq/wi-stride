package io.github.daredevilq.wistride.ui

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
    val current =
        TopLevelDestination.entries.firstOrNull { tab ->
            destination?.hierarchy?.any { it.hasRoute(tab.routeClass) } == true
        }
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
                    current = current,
                    isSettings = destination?.hasRoute(SettingsRoute::class) == true,
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
    current: TopLevelDestination?,
    isSettings: Boolean,
    onBack: () -> Unit,
    onSettings: () -> Unit,
    onAbout: () -> Unit,
) {
    val titleRes =
        when {
            current != null -> current.label
            isSettings -> R.string.menu_settings
            else -> R.string.menu_about
        }
    TopAppBar(
        title = { Text(stringResource(titleRes)) },
        navigationIcon = {
            if (current == null) {
                IconButton(onClick = onBack) {
                    Icon(
                        painterResource(R.drawable.ic_arrow_back),
                        contentDescription = stringResource(R.string.action_navigate_up),
                    )
                }
            }
        },
        actions = {
            if (current != null) OverflowMenu(onSettings = onSettings, onAbout = onAbout)
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
