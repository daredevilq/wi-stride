package io.github.daredevilq.wistride.core.ui.permission

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PermissionRequirementTest {
    private val requirement =
        PermissionRequirement("nearby", listOf("p"), titleRes = 0, rationaleRes = 0, minSdk = 33, maxSdk = 36)

    @Test
    fun `applicable only within the sdk range`() {
        assertThat(requirement.isApplicable(32)).isFalse()
        assertThat(requirement.isApplicable(33)).isTrue()
        assertThat(requirement.isApplicable(36)).isTrue()
        assertThat(requirement.isApplicable(37)).isFalse()
    }
}
