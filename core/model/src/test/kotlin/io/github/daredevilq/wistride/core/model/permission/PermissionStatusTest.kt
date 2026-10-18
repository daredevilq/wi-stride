package io.github.daredevilq.wistride.core.model.permission

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PermissionStatusTest {
    @Test
    fun `granted wins regardless of other flags`() {
        for (rationale in listOf(false, true)) {
            for (requested in listOf(false, true)) {
                assertThat(resolvePermissionStatus(true, rationale, requested))
                    .isEqualTo(PermissionStatus.Granted)
            }
        }
    }

    @Test
    fun `rationale means denied regardless of history`() {
        assertThat(resolvePermissionStatus(false, true, false)).isEqualTo(PermissionStatus.Denied)
        assertThat(resolvePermissionStatus(false, true, true)).isEqualTo(PermissionStatus.Denied)
    }

    @Test
    fun `no rationale after a request means permanently denied`() {
        assertThat(resolvePermissionStatus(false, false, true))
            .isEqualTo(PermissionStatus.PermanentlyDenied)
    }

    @Test
    fun `no rationale and never requested means not requested`() {
        assertThat(resolvePermissionStatus(false, false, false))
            .isEqualTo(PermissionStatus.NotRequested)
    }
}
