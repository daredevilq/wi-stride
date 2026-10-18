package io.github.daredevilq.wistride

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import io.github.daredevilq.wistride.core.designsystem.theme.WiStrideTheme
import io.github.daredevilq.wistride.feature.map.mapScreen
import io.github.daredevilq.wistride.feature.networks.networksScreen
import io.github.daredevilq.wistride.feature.now.NowRoute
import io.github.daredevilq.wistride.feature.now.nowScreen
import io.github.daredevilq.wistride.feature.ping.pingScreen
import io.github.daredevilq.wistride.feature.speed.speedScreen

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WiStrideTheme {
                NavHost(navController = rememberNavController(), startDestination = NowRoute) {
                    nowScreen()
                    pingScreen()
                    networksScreen()
                    speedScreen()
                    mapScreen()
                }
            }
        }
    }
}
