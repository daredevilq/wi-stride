package io.github.daredevilq.wistride.core.ui.permission

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class RecordPermissionResultTest {
    @Test
    fun `dismissed dialog (empty result) is not recorded as a request`() =
        runTest {
            val history = InMemoryPermissionRequestHistory()

            recordPermissionResult(history, "location", emptyMap())

            assertThat(history.wasRequested("location")).isFalse()
        }

    @Test
    fun `denied result is recorded as a request`() =
        runTest {
            val history = InMemoryPermissionRequestHistory()

            recordPermissionResult(history, "location", mapOf("android.permission.ACCESS_FINE_LOCATION" to false))

            assertThat(history.wasRequested("location")).isTrue()
        }

    @Test
    fun `granted result is recorded as a request`() =
        runTest {
            val history = InMemoryPermissionRequestHistory()

            recordPermissionResult(history, "location", mapOf("android.permission.ACCESS_FINE_LOCATION" to true))

            assertThat(history.wasRequested("location")).isTrue()
        }
}
