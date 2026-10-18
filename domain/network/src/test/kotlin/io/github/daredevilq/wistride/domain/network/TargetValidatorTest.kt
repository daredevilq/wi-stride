package io.github.daredevilq.wistride.domain.network

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TargetValidatorTest {
    private fun valid(
        host: String,
        port: String = "",
    ) = (validateTarget(host, port) as? TargetValidation.Valid)

    private fun errorOf(
        host: String,
        port: String = "",
    ) = (validateTarget(host, port) as? TargetValidation.Invalid)?.error

    @Test
    fun acceptedHostsAreNormalized() {
        val cases =
            listOf(
                "8.8.8.8" to "8.8.8.8",
                "  1.1.1.1  " to "1.1.1.1",
                "0.0.0.0" to "0.0.0.0",
                "255.255.255.255" to "255.255.255.255",
                "google.com" to "google.com",
                "Google.COM" to "google.com",
                "google.com." to "google.com",
                "router" to "router",
                "my-host.example.org" to "my-host.example.org",
                "a.b" to "a.b",
                "1host.example" to "1host.example",
                "xn--bcher-kva.example" to "xn--bcher-kva.example",
                "::1" to "::1",
                "::" to "::",
                "2001:db8::1" to "2001:db8::1",
                "2001:DB8:0:0:0:0:0:1" to "2001:db8:0:0:0:0:0:1",
                "[2001:db8::1]" to "2001:db8::1",
                "fe80::1%wlan0" to "fe80::1%wlan0",
                "::ffff:192.168.1.1" to "::ffff:192.168.1.1",
                "64:ff9b::1.2.3.4" to "64:ff9b::1.2.3.4",
                "1:2:3:4:5:6:1.2.3.4" to "1:2:3:4:5:6:1.2.3.4",
                "1:2:3:4:5:6:7::" to "1:2:3:4:5:6:7::",
                "::2:3:4:5:6:7:8" to "::2:3:4:5:6:7:8",
            )
        for ((input, expected) in cases) {
            assertThat(valid(input)?.host).isEqualTo(expected)
        }
    }

    @Test
    fun rejectedHosts() {
        val cases =
            listOf(
                "256.1.1.1",
                "1.2.3",
                "1.2.3.4.5",
                "999",
                "-bad.com",
                "bad-.com",
                "bad..com",
                ".bad.com",
                "bad_host.com",
                "bad host",
                "http://google.com",
                "google.com/path",
                "google.com:443",
                "göogle.com",
                "a".repeat(64) + ".com",
                List(60) { "abcd" }.joinToString("."),
                ":",
                ":::",
                "1:2:3:4:5:6:7",
                "1:2:3:4:5:6:7:8:9",
                "1::2::3",
                "12345::1",
                "g::1",
                "1.2.3.4::",
                "::1.2.3.256",
                "[::1",
                "::1]",
                "[google.com]",
                "fe80::1%",
                "fe80::1%wl an0",
                "%wlan0",
                "1:2:3:4:5:6:7::8",
                "1:2:3:4:5:6:7:8::",
                "2001:db8:::1",
                ":2001:db8::1",
            )
        for (input in cases) {
            assertThat(errorOf(input)).isEqualTo(TargetInputError.InvalidHost)
        }
    }

    @Test
    fun maxLengthHostNameIsAcceptedAndOneMoreIsNot() {
        val label = "a".repeat(MAX_LABEL)
        val ok = listOf(label, label, label, "a".repeat(MAX_HOST - 3 * (MAX_LABEL + 1))).joinToString(".")
        assertThat(ok.length).isEqualTo(MAX_HOST)
        assertThat(valid(ok)?.host).isEqualTo(ok)
        assertThat(errorOf("a$ok")).isEqualTo(TargetInputError.InvalidHost)
    }

    @Test
    fun blankHostIsEmpty() {
        assertThat(errorOf("")).isEqualTo(TargetInputError.EmptyHost)
        assertThat(errorOf("   ")).isEqualTo(TargetInputError.EmptyHost)
    }

    @Test
    fun portParsing() {
        assertThat(valid("host", "")?.port).isNull()
        assertThat(valid("host", "  ")?.port).isNull()
        assertThat(valid("host", "1")?.port).isEqualTo(1)
        assertThat(valid("host", " 443 ")?.port).isEqualTo(443)
        assertThat(valid("host", "65535")?.port).isEqualTo(65_535)
        for (bad in listOf("0", "65536", "-1", "+80", "80.5", "abc", "99999999999", "4 43")) {
            assertThat(errorOf("host", bad)).isEqualTo(TargetInputError.InvalidPort)
        }
    }

    @Test
    fun hostErrorWinsOverPortError() {
        assertThat(errorOf("", "abc")).isEqualTo(TargetInputError.EmptyHost)
        assertThat(errorOf("bad host", "abc")).isEqualTo(TargetInputError.InvalidHost)
    }

    @Test
    fun ipLiteralsAreToldApartFromHostNames() {
        for (literal in listOf("8.8.8.8", "192.168.1.1", "2001:4860:4860::8888", "fe80::1%wlan0")) {
            assertThat(isIpLiteral(literal)).isTrue()
        }
        for (name in listOf("google.com", "router", "1.2.3", "x.com")) {
            assertThat(isIpLiteral(name)).isFalse()
        }
    }

    private companion object {
        const val MAX_LABEL = 63
        const val MAX_HOST = 253
    }
}
