package io.github.daredevilq.wistride.core.wifi

import io.github.daredevilq.wistride.core.common.Dispatcher
import io.github.daredevilq.wistride.core.common.WiStrideDispatcher
import io.github.daredevilq.wistride.core.model.wifi.WifiSnapshot
import io.github.daredevilq.wistride.core.wifi.di.SdkInt
import io.github.daredevilq.wistride.core.wifi.source.ConnectivitySource
import io.github.daredevilq.wistride.core.wifi.source.ScanResultsSource
import io.github.daredevilq.wistride.core.wifi.source.WifiPermissionChecker
import io.github.daredevilq.wistride.core.wifi.source.WifiStateSource
import io.github.daredevilq.wistride.domain.wifi.normalizeBssid
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

internal class DefaultWifiConnectionRepository
    @Inject
    constructor(
        private val stateSource: WifiStateSource,
        private val connectivitySource: ConnectivitySource,
        private val scanSource: ScanResultsSource,
        private val permissions: WifiPermissionChecker,
        @SdkInt private val sdkInt: Int,
        @Dispatcher(WiStrideDispatcher.Default) private val defaultDispatcher: CoroutineDispatcher,
    ) : WifiConnectionRepository {
        override val snapshots: Flow<WifiSnapshot> =
            flow {
                // Per collection: one scan request at most, to learn the channel width of the connected AP.
                var scanRequested = false
                val inputs =
                    combine(
                        stateSource.wifiEnabled,
                        stateSource.airplaneModeOn,
                        stateSource.locationEnabled,
                        connectivitySource.wifiNetwork,
                        scanSource.scanResults,
                    ) { wifiEnabled, airplaneModeOn, locationEnabled, network, scans ->
                        val access =
                            WifiAccess(
                                hasFineLocation = permissions.hasFineLocation(),
                                canReadScanResults = permissions.canReadScanResults(),
                                locationEnabled = locationEnabled,
                                sdkInt = sdkInt,
                            )
                        val connectedBssid = network?.wifiInfo?.bssid?.let(::normalizeBssid)
                        val missingFromCache =
                            connectedBssid != null && scans.none { it.bssid.equals(connectedBssid, ignoreCase = true) }
                        if (!scanRequested && missingFromCache && access.canReadScanResults) {
                            scanRequested = true
                            scanSource.requestScan()
                        }
                        buildSnapshot(wifiEnabled, airplaneModeOn, network, scans, access)
                    }
                emitAll(inputs)
            }.distinctUntilChanged().flowOn(defaultDispatcher)
    }
