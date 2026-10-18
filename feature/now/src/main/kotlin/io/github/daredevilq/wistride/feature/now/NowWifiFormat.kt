package io.github.daredevilq.wistride.feature.now

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.daredevilq.wistride.core.model.wifi.ChannelWidth
import io.github.daredevilq.wistride.core.model.wifi.IpAddressWithPrefix
import io.github.daredevilq.wistride.core.model.wifi.WifiBand
import io.github.daredevilq.wistride.core.model.wifi.WifiGeneration
import io.github.daredevilq.wistride.core.model.wifi.WifiSecurity
import io.github.daredevilq.wistride.core.model.wifi.WifiStandard

@Composable
internal fun WifiBand.text(): String =
    stringResource(
        when (this) {
            WifiBand.Ghz2Point4 -> R.string.now_band_2_4
            WifiBand.Ghz5 -> R.string.now_band_5
            WifiBand.Ghz6 -> R.string.now_band_6
        },
    )

@Composable
internal fun ChannelWidth.text(): String =
    if (this == ChannelWidth.Mhz80Plus80) {
        stringResource(R.string.now_value_width_80_80)
    } else {
        stringResource(R.string.now_value_mhz, totalMhz)
    }

@Composable
internal fun GenerationInfo.text(): String =
    stringResource(R.string.now_value_generation, generation.text(), standard.text())

@Composable
private fun WifiGeneration.text(): String =
    stringResource(
        when (this) {
            WifiGeneration.Legacy -> R.string.now_generation_legacy
            WifiGeneration.WiFi4 -> R.string.now_generation_wifi4
            WifiGeneration.WiFi5 -> R.string.now_generation_wifi5
            WifiGeneration.WiFi6 -> R.string.now_generation_wifi6
            WifiGeneration.WiFi6E -> R.string.now_generation_wifi6e
            WifiGeneration.WiFi7 -> R.string.now_generation_wifi7
            WifiGeneration.WiGig -> R.string.now_generation_wigig
        },
    )

@Composable
private fun WifiStandard.text(): String =
    stringResource(
        when (this) {
            WifiStandard.Legacy -> R.string.now_standard_legacy
            WifiStandard.Ieee80211n -> R.string.now_standard_n
            WifiStandard.Ieee80211ac -> R.string.now_standard_ac
            WifiStandard.Ieee80211ax -> R.string.now_standard_ax
            WifiStandard.Ieee80211ad -> R.string.now_standard_ad
            WifiStandard.Ieee80211be -> R.string.now_standard_be
        },
    )

@Composable
internal fun WifiSecurity.text(): String =
    stringResource(
        when (this) {
            WifiSecurity.Open -> R.string.now_security_open
            WifiSecurity.Wep -> R.string.now_security_wep
            WifiSecurity.Psk -> R.string.now_security_psk
            WifiSecurity.Eap -> R.string.now_security_eap
            WifiSecurity.Sae -> R.string.now_security_sae
            WifiSecurity.Wpa3Enterprise -> R.string.now_security_wpa3_enterprise
            WifiSecurity.Wpa3Enterprise192Bit -> R.string.now_security_wpa3_enterprise_192
            WifiSecurity.Owe -> R.string.now_security_owe
            WifiSecurity.WapiPsk -> R.string.now_security_wapi_psk
            WifiSecurity.WapiCert -> R.string.now_security_wapi_cert
            WifiSecurity.Osen -> R.string.now_security_osen
            WifiSecurity.Passpoint -> R.string.now_security_passpoint
            WifiSecurity.Dpp -> R.string.now_security_dpp
        },
    )

@Composable
internal fun IpAddressWithPrefix.text(): String = stringResource(R.string.now_value_ip_prefix, address, prefixLength)
