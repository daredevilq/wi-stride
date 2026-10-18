package io.github.daredevilq.wistride.core.wifi

import io.github.daredevilq.wistride.core.model.wifi.ConnectionState
import io.github.daredevilq.wistride.core.model.wifi.IpAddressWithPrefix
import io.github.daredevilq.wistride.core.model.wifi.WifiConnection
import io.github.daredevilq.wistride.core.model.wifi.WifiSnapshot
import io.github.daredevilq.wistride.core.wifi.source.RawLinkInfo
import io.github.daredevilq.wistride.core.wifi.source.RawScanResult
import io.github.daredevilq.wistride.core.wifi.source.RawWifiInfo
import io.github.daredevilq.wistride.core.wifi.source.WifiNetworkInfo
import io.github.daredevilq.wistride.domain.wifi.AndroidWifiConstants as K

internal const val BSSID = "AA:BB:CC:DD:EE:FF"

internal fun rawWifiInfo(
    ssid: String? = "\"Home\"",
    bssid: String? = BSSID,
    rssi: Int = -55,
    frequencyMhz: Int = 5180,
    tx: Int = 433,
    rx: Int = 390,
    maxTx: Int? = 866,
    maxRx: Int? = 866,
    standard: Int? = K.WIFI_STANDARD_11AC,
    security: Int? = K.SECURITY_TYPE_SAE,
) = RawWifiInfo(ssid, bssid, rssi, frequencyMhz, tx, rx, maxTx, maxRx, standard, security)

internal fun rawLink(
    ipv4: List<IpAddressWithPrefix> = listOf(IpAddressWithPrefix("192.168.1.23", 24)),
    ipv6: List<IpAddressWithPrefix> = listOf(IpAddressWithPrefix("fe80::1", 64)),
    gateway: String? = "192.168.1.1",
    dns: List<String> = listOf("192.168.1.1"),
    mtu: Int = 1500,
) = RawLinkInfo(ipv4, ipv6, gateway, dns, mtu)

internal fun rawScan(
    bssid: String = BSSID,
    channelWidth: Int = K.CHANNEL_WIDTH_80MHZ,
) = RawScanResult(bssid, "Home", -50, 5180, channelWidth, 5210, 0, 0L)

internal fun network(
    info: RawWifiInfo? = rawWifiInfo(),
    link: RawLinkInfo? = rawLink(),
) = WifiNetworkInfo(info, link)

internal fun WifiSnapshot.connection(): WifiConnection = (state as ConnectionState.Connected).connection
