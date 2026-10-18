package io.github.daredevilq.wistride.core.ui.permission

import android.Manifest
import android.app.Application
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.core.app.ActivityOptionsCompat
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import io.github.daredevilq.wistride.core.model.permission.InMemoryPermissionRequestHistory
import io.github.daredevilq.wistride.core.model.permission.PermissionRequestHistory
import io.github.daredevilq.wistride.core.model.permission.PermissionSnapshot
import io.github.daredevilq.wistride.core.model.permission.PermissionStatus
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/** Records launches and lets the test play the system dialog's answer. */
private class FakeActivityResultRegistry : ActivityResultRegistry() {
    val launchedInputs = mutableListOf<List<String>>()
    private var requestCode = 0

    override fun <I, O> onLaunch(
        requestCode: Int,
        contract: ActivityResultContract<I, O>,
        input: I,
        options: ActivityOptionsCompat?,
    ) {
        this.requestCode = requestCode
        @Suppress("UNCHECKED_CAST") // RequestMultiplePermissions takes Array<String>.
        launchedInputs += (input as Array<String>).toList()
    }

    fun answer(result: Map<String, Boolean>) {
        dispatchResult(requestCode, result)
    }
}

@RunWith(RobolectricTestRunner::class)
class PermissionRequestStateTest {
    @get:Rule
    val rule = createComposeRule()

    private val registry = FakeActivityResultRegistry()
    private val fine = Manifest.permission.ACCESS_FINE_LOCATION
    private val coarse = Manifest.permission.ACCESS_COARSE_LOCATION
    private val requirement =
        PermissionRequirement(
            id = "test",
            permissions = listOf(fine, coarse),
            titleRes = android.R.string.ok,
            rationaleRes = android.R.string.ok,
        )
    private val seen = mutableListOf<PermissionSnapshot?>()
    private lateinit var state: PermissionRequestState

    private fun setContent(history: PermissionRequestHistory) {
        val owner =
            object : ActivityResultRegistryOwner {
                override val activityResultRegistry: ActivityResultRegistry = registry
            }
        rule.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner) {
                state = rememberPermissionRequestState(requirement, history)
                SideEffect { seen += state.snapshot }
            }
        }
    }

    private fun waitForStatus(status: PermissionStatus) = rule.waitUntil { state.snapshot?.status == status }

    private fun grant(vararg permissions: String) {
        shadowOf(ApplicationProvider.getApplicationContext<Application>()).grantPermissions(*permissions)
    }

    @Test
    fun launchRequestLaunchesExactlyTheRequirementsPermissions() {
        setContent(InMemoryPermissionRequestHistory())
        waitForStatus(PermissionStatus.NotRequested)
        rule.runOnIdle { state.launchRequest() }
        assertThat(registry.launchedInputs).containsExactly(listOf(fine, coarse))
    }

    @Test
    fun decisionIsRecordedAndSnapshotRefreshed() {
        val history = InMemoryPermissionRequestHistory()
        setContent(history)
        waitForStatus(PermissionStatus.NotRequested)
        rule.runOnIdle { state.launchRequest() }

        rule.runOnIdle { registry.answer(mapOf(fine to false, coarse to false)) }
        waitForStatus(PermissionStatus.PermanentlyDenied)
        assertThat(runBlocking { history.wasRequested("test") }).isTrue()
        assertThat(state.snapshot?.granted).isEmpty()
    }

    @Test
    fun grantingOnlyOnePermissionKeepsTheStatusButChangesTheGrantedSet() {
        val history = InMemoryPermissionRequestHistory(setOf("test"))
        setContent(history)
        waitForStatus(PermissionStatus.PermanentlyDenied)

        grant(coarse)
        rule.runOnIdle { state.launchRequest() }
        rule.runOnIdle { registry.answer(mapOf(fine to false, coarse to true)) }
        rule.waitUntil { state.snapshot?.granted == setOf(coarse) }
        assertThat(state.snapshot?.status).isEqualTo(PermissionStatus.PermanentlyDenied)
    }

    @Test
    fun grantedSnapshotIsKnownSynchronouslyAndNeverDenied() {
        grant(fine, coarse)
        setContent(InMemoryPermissionRequestHistory())
        rule.waitForIdle()
        assertThat(seen.first()).isEqualTo(PermissionSnapshot(PermissionStatus.Granted, setOf(fine, coarse)))
        assertThat(seen.map { it?.status }.toSet()).containsExactly(PermissionStatus.Granted)
    }

    @Test
    fun dismissedDialogRecordsNothing() {
        val history = InMemoryPermissionRequestHistory()
        setContent(history)
        waitForStatus(PermissionStatus.NotRequested)
        rule.runOnIdle { state.launchRequest() }

        rule.runOnIdle { registry.answer(emptyMap()) }
        rule.waitForIdle()
        assertThat(runBlocking { history.wasRequested("test") }).isFalse()
        assertThat(state.snapshot?.status).isEqualTo(PermissionStatus.NotRequested)
    }

    @Test
    fun previouslyRequestedAndDeniedResolvesWithoutEverShowingNotRequested() {
        setContent(InMemoryPermissionRequestHistory(setOf("test")))
        waitForStatus(PermissionStatus.PermanentlyDenied)
        assertThat(seen.first()).isNull()
        assertThat(seen.mapNotNull { it?.status }).doesNotContain(PermissionStatus.NotRequested)
    }
}
