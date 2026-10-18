package io.github.daredevilq.wistride.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.github.daredevilq.wistride.R
import io.github.daredevilq.wistride.feature.map.MapRoute
import io.github.daredevilq.wistride.feature.networks.NetworksRoute
import io.github.daredevilq.wistride.feature.now.NowRoute
import io.github.daredevilq.wistride.feature.ping.PingRoute
import io.github.daredevilq.wistride.feature.speed.SpeedRoute

/** Tabs shown in the bottom bar / navigation rail. */
enum class TopLevelDestination(
    val route: Any,
    @DrawableRes val icon: Int,
    @StringRes val label: Int,
) {
    NOW(NowRoute, R.drawable.ic_wifi, R.string.nav_now),
    PING(PingRoute, R.drawable.ic_ping, R.string.nav_ping),
    NETWORKS(NetworksRoute, R.drawable.ic_networks, R.string.nav_networks),
    SPEED(SpeedRoute, R.drawable.ic_speed, R.string.nav_speed),
    MAP(MapRoute, R.drawable.ic_map, R.string.nav_map),
}
