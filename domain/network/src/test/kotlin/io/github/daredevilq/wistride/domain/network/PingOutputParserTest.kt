package io.github.daredevilq.wistride.domain.network

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.Locale

class PingOutputParserTest {
    private fun parse(line: String) = PingOutputParser.parsePingLine(line)

    @Test
    fun `parses a plain reply`() {
        assertThat(parse("64 bytes from 8.8.8.8: icmp_seq=1 ttl=113 time=14.2 ms"))
            .isEqualTo(PingLine.Reply(1, 14.2, 113, duplicate = false))
    }

    @Test
    fun `parses sub-millisecond time`() {
        assertThat(parse("64 bytes from 192.168.1.1: icmp_seq=7 ttl=64 time=0.045 ms"))
            .isEqualTo(PingLine.Reply(7, 0.045, 64, duplicate = false))
    }

    @Test
    fun `parses time less than one millisecond as upper bound`() {
        assertThat(parse("64 bytes from 192.168.1.1: icmp_seq=7 ttl=64 time<1 ms"))
            .isEqualTo(PingLine.Reply(7, 1.0, 64, duplicate = false))
    }

    @Test
    fun `flags duplicates`() {
        val line = parse("64 bytes from 8.8.8.8: icmp_seq=3 ttl=113 time=14.7 ms (DUP!)")
        assertThat((line as PingLine.Reply).duplicate).isTrue()
    }

    @Test
    fun `accepts epoch prefix`() {
        assertThat(parse("[1792314001.676483] 64 bytes from 8.8.8.8: icmp_seq=2 ttl=113 time=9.5 ms"))
            .isEqualTo(PingLine.Reply(2, 9.5, 113, duplicate = false))
    }

    @Test
    fun `accepts host name in reply line`() {
        val line = "64 bytes from zr-in-f102.1e100.net (142.250.109.102): icmp_seq=1 ttl=111 time=9.05 ms"
        assertThat(parse(line)).isEqualTo(PingLine.Reply(1, 9.05, 111, duplicate = false))
    }

    @Test
    fun `reply without ttl`() {
        assertThat((parse("64 bytes from 8.8.8.8: icmp_seq=1 time=5 ms") as PingLine.Reply).ttl).isNull()
    }

    @Test
    fun `parses no answer with and without prefix`() {
        assertThat(parse("[1792314001.676483] no answer yet for icmp_seq=1")).isEqualTo(PingLine.NoAnswer(1))
        assertThat(parse("no answer yet for icmp_seq=12")).isEqualTo(PingLine.NoAnswer(12))
    }

    @Test
    fun `parses unreachable`() {
        assertThat(parse("From 192.168.1.5 icmp_seq=4 Destination Host Unreachable"))
            .isEqualTo(PingLine.Unreachable(4))
        assertThat(parse("From 10.0.0.1 icmp_seq=2 Destination Net Unreachable"))
            .isEqualTo(PingLine.Unreachable(2))
    }

    @Test
    fun `parses unknown host`() {
        assertThat(parse("ping: unknown host nonexistent.invalid")).isEqualTo(PingLine.UnknownHost)
    }

    @Test
    fun `ignores everything else`() {
        listOf(
            "",
            "PING 8.8.8.8 (8.8.8.8) 56(84) bytes of data.",
            "--- 8.8.8.8 ping statistics ---",
            "3 packets transmitted, 3 received, 0% packet loss, time 1002ms",
            "rtt min/avg/max/mdev = 14.282/14.536/14.759/0.240 ms",
            "connect: Network is unreachable",
            "ping: cannot flood; minimal interval, allowed for user, is 200ms",
        ).forEach { assertThat(parse(it)).isEqualTo(PingLine.Ignored) }
    }

    @Test
    fun `number parsing ignores the default locale`() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("pl-PL"))
            val line = parse("64 bytes from 8.8.8.8: icmp_seq=1 ttl=1 time=14.2 ms") as PingLine.Reply
            assertThat(line.rttMs).isEqualTo(14.2)
        } finally {
            Locale.setDefault(previous)
        }
    }
}
