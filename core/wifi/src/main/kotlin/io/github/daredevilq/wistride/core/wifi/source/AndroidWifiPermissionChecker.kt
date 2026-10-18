package io.github.daredevilq.wistride.core.wifi.source

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.daredevilq.wistride.core.wifi.di.SdkInt
import javax.inject.Inject

internal class AndroidWifiPermissionChecker
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        @SdkInt private val sdkInt: Int,
    ) : WifiPermissionChecker {
        override fun hasFineLocation(): Boolean = isGranted(Manifest.permission.ACCESS_FINE_LOCATION)

        override fun canReadScanResults(): Boolean =
            isGranted(
                if (sdkInt >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.NEARBY_WIFI_DEVICES
                } else {
                    Manifest.permission.ACCESS_FINE_LOCATION
                },
            )

        private fun isGranted(permission: String): Boolean =
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
