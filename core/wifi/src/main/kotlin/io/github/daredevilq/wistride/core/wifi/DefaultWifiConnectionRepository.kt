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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
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
        @OptIn(ExperimentalCoroutinesApi::class) // flatMapLatest: re-subscribe on location changes.
        override val snapshots: Flow<WifiSnapshot> =
            flow {
                // Per collection: one scan request at most, to learn the channel width of the connected AP.
                var scanRequested = false
                // Redaction is decided when the network callback is registered: an already registered
                // callback keeps delivering SSID/BSSID after location is switched off (seen on the A53,
                // API 36). Re-registering on every location change keeps the values consistent with it.
                val networkPerLocationMode =
                    stateSource.locationEnabled.flatMapLatest { locationEnabled ->
                        connectivitySource.wifiNetwork.map { network -> locationEnabled to network }
                    }
                val inputs =
                    combine(
                        stateSource.wifiEnabled,
                        stateSource.airplaneModeOn,
                        networkPerLocationMode,
                        scanSource.scanResults,
                    ) { wifiEnabled, airplaneModeOn, (locationEnabled, network), scans ->
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
                            scanRequested = scanSource.requestScan()
                        }
                        buildSnapshot(wifiEnabled, airplaneModeOn, network, scans, access)
                    }
                emitAll(inputs)
            }.distinctUntilChanged().flowOn(defaultDispatcher)
    }
