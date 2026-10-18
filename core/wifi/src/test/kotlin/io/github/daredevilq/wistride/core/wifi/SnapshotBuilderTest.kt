package io.github.daredevilq.wistride.core.wifi

import android.Manifest
import com.google.common.truth.Truth.assertThat
import io.github.daredevilq.wistride.core.model.Availability
import io.github.daredevilq.wistride.core.model.UnavailableReason
import io.github.daredevilq.wistride.core.model.available
import io.github.daredevilq.wistride.core.model.unavailable
import io.github.daredevilq.wistride.core.model.wifi.ChannelWidth
import io.github.daredevilq.wistride.core.model.wifi.ConnectionState
import io.github.daredevilq.wistride.core.model.wifi.IpAddressWithPrefix
import io.github.daredevilq.wistride.core.model.wifi.WifiEnvironment
import io.github.daredevilq.wistride.core.model.wifi.WifiSecurity
import io.github.daredevilq.wistride.core.model.wifi.WifiStandard
import io.github.daredevilq.wistride.core.wifi.source.WifiNetworkInfo
import org.junit.Test
import io.github.daredevilq.wistride.domain.wifi.AndroidWifiConstants as K

private const val FINE = Manifest.permission.ACCESS_FINE_LOCATION
private const val NEARBY = Manifest.permission.NEARBY_WIFI_DEVICES

class SnapshotBuilderTest {
    private fun build(
        network: WifiNetworkInfo? = network(),
        scans: List<io.github.daredevilq.wistride.core.wifi.source.RawScanResult> = listOf(rawScan()),
        wifiEnabled: Boolean = true,
        airplane: Boolean = false,
        locationEnabled: Boolean = true,
        hasFine: Boolean = true,
        canScan: Boolean = true,
        sdk: Int = 36,
    ) = buildSnapshot(wifiEnabled, airplane, network, scans, WifiAccess(hasFine, canScan, locationEnabled, sdk))

    private fun reason(r: UnavailableReason) = r.unavailable()

    @Test
    fun stateAirplaneMode() {
        val s = build(network = null, wifiEnabled = false, airplane = true)
        assertThat(s.state).isEqualTo(ConnectionState.AirplaneMode)
        assertThat(s.environment).isEqualTo(WifiEnvironment(false, true, true))
    }

    @Test
    fun stateWifiOff() {
        assertThat(build(network = null, wifiEnabled = false).state).isEqualTo(ConnectionState.WifiOff)
    }

    @Test
    fun stateDisconnected() {
        assertThat(build(network = null).state).isEqualTo(ConnectionState.Disconnected)
    }

    @Test
    fun networkWinsEvenIfRadioFlagLags() {
        assertThat(
            build(wifiEnabled = false, airplane = true).state,
        ).isInstanceOf(ConnectionState.Connected::class.java)
    }

    @Test
    fun fullyAvailableConnection() {
        val c = build().connection()
        assertThat(c.ssid).isEqualTo("Home".available())
        assertThat(c.bssid).isEqualTo(BSSID.lowercase().available())
        assertThat(c.rssiDbm).isEqualTo((-55).available())
        assertThat(c.frequencyMhz).isEqualTo(5180.available())
        assertThat(c.channelWidth).isEqualTo(ChannelWidth.Mhz80.available())
        assertThat(c.standard).isEqualTo(WifiStandard.Ieee80211ac.available())
        assertThat(c.txLinkSpeedMbps).isEqualTo(433.available())
        assertThat(c.rxLinkSpeedMbps).isEqualTo(390.available())
        assertThat(c.maxTxLinkSpeedMbps).isEqualTo(866.available())
        assertThat(c.maxRxLinkSpeedMbps).isEqualTo(866.available())
        assertThat(c.security).isEqualTo(WifiSecurity.Sae.available())
        assertThat(c.ip.ipv4).isEqualTo(IpAddressWithPrefix("192.168.1.23", 24).available())
        assertThat(c.ip.ipv6).containsExactly(IpAddressWithPrefix("fe80::1", 64))
        assertThat(c.ip.gateway).isEqualTo("192.168.1.1".available())
        assertThat(c.ip.dnsServers).isEqualTo(listOf("192.168.1.1").available())
        assertThat(c.ip.mtu).isEqualTo(1500.available())
    }

    @Test
    fun missingWifiInfoIsAwaitingDataEverywhere() {
        val c = build(network = network(info = null)).connection()
        val awaiting = reason(UnavailableReason.AwaitingData)
        listOf(
            c.ssid,
            c.bssid,
            c.rssiDbm,
            c.frequencyMhz,
            c.channelWidth,
            c.standard,
            c.txLinkSpeedMbps,
            c.rxLinkSpeedMbps,
            c.maxTxLinkSpeedMbps,
            c.maxRxLinkSpeedMbps,
            c.security,
        ).forEach { assertThat(it).isEqualTo(awaiting) }
        assertThat(c.ip.ipv4).isInstanceOf(Availability.Available::class.java)
    }

    @Test
    fun missingLinkIsAwaitingDataForIp() {
        val ip = build(network = network(link = null)).connection().ip
        val awaiting = reason(UnavailableReason.AwaitingData)
        assertThat(ip.ipv4).isEqualTo(awaiting)
        assertThat(ip.ipv6).isEmpty()
        assertThat(ip.gateway).isEqualTo(awaiting)
        assertThat(ip.dnsServers).isEqualTo(awaiting)
        assertThat(ip.mtu).isEqualTo(awaiting)
    }

    @Test
    fun redactedIdentityWithoutPermissionNeedsFine() {
        val net = network(info = rawWifiInfo(ssid = "<unknown ssid>", bssid = "02:00:00:00:00:00"))
        val c = build(network = net, hasFine = false, locationEnabled = true).connection()
        val needsFine = reason(UnavailableReason.NeedsPermission(FINE))
        assertThat(c.ssid).isEqualTo(needsFine)
        assertThat(c.bssid).isEqualTo(needsFine)
        assertThat(c.channelWidth).isEqualTo(needsFine)
    }

    @Test
    fun redactedIdentityWithLocationOffNeedsLocation() {
        val net = network(info = rawWifiInfo(ssid = "<unknown ssid>", bssid = "02:00:00:00:00:00"))
        val c = build(network = net, locationEnabled = false).connection()
        assertThat(c.ssid).isEqualTo(reason(UnavailableReason.NeedsLocationOn))
        assertThat(c.bssid).isEqualTo(reason(UnavailableReason.NeedsLocationOn))
        assertThat(c.channelWidth).isEqualTo(reason(UnavailableReason.NeedsLocationOn))
    }

    @Test
    fun redactedIdentityWithEverythingGrantedIsNotReported() {
        val net = network(info = rawWifiInfo(ssid = "<unknown ssid>", bssid = null))
        val c = build(network = net).connection()
        assertThat(c.ssid).isEqualTo(reason(UnavailableReason.NotReportedByDevice))
        assertThat(c.bssid).isEqualTo(reason(UnavailableReason.NotReportedByDevice))
    }

    @Test
    fun sentinelsAreNotReportedNeverZero() {
        val net =
            network(
                info =
                    rawWifiInfo(
                        rssi = K.INVALID_RSSI,
                        frequencyMhz = 0,
                        tx = K.LINK_SPEED_UNKNOWN,
                        rx = 0,
                        maxTx = K.LINK_SPEED_UNKNOWN,
                        maxRx = K.LINK_SPEED_UNKNOWN,
                        standard = K.WIFI_STANDARD_UNKNOWN,
                        security = K.SECURITY_TYPE_UNKNOWN,
                    ),
                link = rawLink(ipv4 = emptyList(), gateway = null, dns = emptyList(), mtu = 0),
            )
        val c = build(network = net, sdk = 36).connection()
        val notReported = reason(UnavailableReason.NotReportedByDevice)
        listOf(
            c.rssiDbm,
            c.frequencyMhz,
            c.txLinkSpeedMbps,
            c.rxLinkSpeedMbps,
            c.maxTxLinkSpeedMbps,
            c.maxRxLinkSpeedMbps,
            c.standard,
            c.security,
            c.ip.ipv4,
            c.ip.gateway,
            c.ip.dnsServers,
            c.ip.mtu,
        ).forEach { assertThat(it).isEqualTo(notReported) }
    }

    @Test
    fun api29HasNoMaxSpeedStandardOrSecurity() {
        val net = network(info = rawWifiInfo(maxTx = null, maxRx = null, standard = null, security = null))
        val c = build(network = net, sdk = 29).connection()
        assertThat(c.maxTxLinkSpeedMbps).isEqualTo(reason(UnavailableReason.RequiresApi(30)))
        assertThat(c.maxRxLinkSpeedMbps).isEqualTo(reason(UnavailableReason.RequiresApi(30)))
        assertThat(c.standard).isEqualTo(reason(UnavailableReason.RequiresApi(30)))
        assertThat(c.security).isEqualTo(reason(UnavailableReason.RequiresApi(31)))
        assertThat(c.txLinkSpeedMbps).isEqualTo(433.available())
    }

    @Test
    fun api30HasStandardButNotSecurity() {
        val c = build(network = network(info = rawWifiInfo(security = null)), sdk = 30).connection()
        assertThat(c.standard).isEqualTo(WifiStandard.Ieee80211ac.available())
        assertThat(c.maxTxLinkSpeedMbps).isEqualTo(866.available())
        assertThat(c.security).isEqualTo(reason(UnavailableReason.RequiresApi(31)))
    }

    @Test
    fun api31HasSecurity() {
        assertThat(build(sdk = 31).connection().security).isEqualTo(WifiSecurity.Sae.available())
    }

    @Test
    fun channelWidthScanPermissionDependsOnApi() {
        assertThat(build(canScan = false, sdk = 32).connection().channelWidth)
            .isEqualTo(reason(UnavailableReason.NeedsPermission(FINE)))
        assertThat(build(canScan = false, sdk = 33).connection().channelWidth)
            .isEqualTo(reason(UnavailableReason.NeedsPermission(NEARBY)))
    }

    @Test
    fun channelWidthNeedsLocationOnlyUpToApi32() {
        assertThat(build(locationEnabled = false, sdk = 32).connection().channelWidth)
            .isEqualTo(reason(UnavailableReason.NeedsLocationOn))
        assertThat(build(locationEnabled = false, sdk = 33).connection().channelWidth)
            .isEqualTo(ChannelWidth.Mhz80.available())
    }

    @Test
    fun channelWidthAwaitsScanForConnectedBssid() {
        val c = build(scans = listOf(rawScan(bssid = "11:22:33:44:55:66"))).connection()
        assertThat(c.channelWidth).isEqualTo(reason(UnavailableReason.AwaitingData))
    }

    @Test
    fun channelWidthMatchesBssidCaseInsensitively() {
        val c =
            build(
                scans = listOf(rawScan(bssid = BSSID.lowercase(), channelWidth = K.CHANNEL_WIDTH_160MHZ)),
            ).connection()
        assertThat(c.channelWidth).isEqualTo(ChannelWidth.Mhz160.available())
    }

    @Test
    fun unknownChannelWidthValueIsNotReported() {
        val c = build(scans = listOf(rawScan(channelWidth = 99))).connection()
        assertThat(c.channelWidth).isEqualTo(reason(UnavailableReason.NotReportedByDevice))
    }

    @Test
    fun wifiWithoutInternetStillShowsIpConfiguration() {
        // The NetworkRequest has no INTERNET capability, so such a network arrives like any other.
        val c = build(network = network(link = rawLink(gateway = null, dns = emptyList()))).connection()
        assertThat(c.ip.ipv4).isInstanceOf(Availability.Available::class.java)
        assertThat(c.ip.gateway).isEqualTo(reason(UnavailableReason.NotReportedByDevice))
    }
}
