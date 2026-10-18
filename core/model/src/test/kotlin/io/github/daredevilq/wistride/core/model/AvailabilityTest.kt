package io.github.daredevilq.wistride.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AvailabilityTest {
    @Test
    fun `available wraps the value`() {
        assertThat(5.available()).isEqualTo(Availability.Available(5))
    }

    @Test
    fun `unavailable wraps the reason`() {
        assertThat(UnavailableReason.NeedsLocationOn.unavailable())
            .isEqualTo(Availability.Unavailable(UnavailableReason.NeedsLocationOn))
    }

    @Test
    fun `valueOrNull returns value or null`() {
        assertThat("x".available().valueOrNull()).isEqualTo("x")
        val missing: Availability<String> = UnavailableReason.RequiresApi(31).unavailable()
        assertThat(missing.valueOrNull()).isNull()
    }

    @Test
    fun `map transforms available values`() {
        assertThat(2.available().map { it * 3 }).isEqualTo(6.available())
    }

    @Test
    fun `map keeps the unavailable reason`() {
        val source: Availability<Int> = UnavailableReason.NotReportedByDevice.unavailable()
        assertThat(source.map { it + 1 })
            .isEqualTo(Availability.Unavailable(UnavailableReason.NotReportedByDevice))
    }
}
