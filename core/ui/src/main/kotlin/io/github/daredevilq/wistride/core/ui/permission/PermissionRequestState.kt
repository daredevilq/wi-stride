package io.github.daredevilq.wistride.core.ui.permission

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import io.github.daredevilq.wistride.core.model.permission.PermissionRequestHistory
import io.github.daredevilq.wistride.core.model.permission.PermissionSnapshot
import io.github.daredevilq.wistride.core.model.permission.PermissionStatus
import io.github.daredevilq.wistride.core.model.permission.resolvePermissionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Observable status of one [PermissionRequirement] plus the actions to improve it. */
@Stable
interface PermissionRequestState {
    /**
     * Latest reading; updated after the system dialog and when returning from settings.
     *
     * `null` means "still resolving": the request history has not been read yet and the platform
     * signals alone are ambiguous (denied, no rationale: never asked or "don't ask again"). Render
     * neither the rationale card nor the gated content until it is non-null, otherwise the first
     * frame would flash the wrong state.
     */
    val snapshot: PermissionSnapshot?

    /** Shows the system permission dialog. */
    fun launchRequest()

    /** Opens the app's system settings page, the only way out of [PermissionStatus.PermanentlyDenied]. */
    fun openAppSettings()
}

/**
 * Creates a [PermissionRequestState] for [requirement], re-evaluated on every `ON_RESUME`
 * so a grant made in system settings is picked up when the user comes back.
 */
@Composable
fun rememberPermissionRequestState(
    requirement: PermissionRequirement,
    history: PermissionRequestHistory,
): PermissionRequestState {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state =
        remember(requirement, history, context) {
            DefaultPermissionRequestState(requirement, history, context, scope)
        }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            scope.launch {
                recordPermissionResult(history, requirement.id, result)
                state.refresh()
            }
        }
    SideEffect { state.launcher = launcher }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        scope.launch { state.refresh() }
    }
    return state
}

/**
 * Records a request only when the system dialog produced a decision. On API 30+ dismissing the
 * dialog (back / tap outside) returns an empty result and is not a denial; recording it would make
 * the next evaluation report "don't ask again" although the dialog can still be shown.
 */
internal suspend fun recordPermissionResult(
    history: PermissionRequestHistory,
    requirementId: String,
    result: Map<String, Boolean>,
) {
    if (result.isNotEmpty()) history.markRequested(requirementId)
}

private class DefaultPermissionRequestState(
    private val requirement: PermissionRequirement,
    private val history: PermissionRequestHistory,
    private val context: Context,
    private val scope: CoroutineScope,
) : PermissionRequestState {
    var launcher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>? = null

    // Only the unambiguous cases are known synchronously (all granted, or the rationale is due);
    // everything else needs the persisted history and stays null until the first refresh().
    override var snapshot by mutableStateOf(initialSnapshot())
        private set

    // Evaluations may be triggered concurrently (launcher callback and ON_RESUME); serializing them
    // makes each one read fresh values, so the last evaluation to run always wins.
    private val refreshMutex = Mutex()

    override fun launchRequest() {
        launcher?.launch(requirement.permissions.toTypedArray())
    }

    override fun openAppSettings() {
        val details =
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(details)
        } catch (_: ActivityNotFoundException) {
            // Some OEM builds have no per-app details screen; the general settings are the best fallback.
            context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    suspend fun refresh() {
        refreshMutex.withLock {
            val granted = grantedPermissions()
            val status =
                resolvePermissionStatus(
                    isGranted = isGranted(granted),
                    shouldShowRationale = shouldShowRationale(),
                    wasRequestedBefore = history.wasRequested(requirement.id),
                )
            snapshot = PermissionSnapshot(status, granted)
        }
    }

    private fun initialSnapshot(): PermissionSnapshot? {
        val granted = grantedPermissions()
        return when {
            isGranted(granted) -> PermissionSnapshot(PermissionStatus.Granted, granted)
            shouldShowRationale() -> PermissionSnapshot(PermissionStatus.Denied, granted)
            else -> null
        }
    }

    private fun grantedPermissions(): Set<String> =
        if (requirement.isApplicable(Build.VERSION.SDK_INT)) {
            requirement.permissions
                .filter { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
                .toSet()
        } else {
            requirement.permissions.toSet()
        }

    private fun isGranted(granted: Set<String>): Boolean = granted.containsAll(requirement.permissions)

    private fun shouldShowRationale(): Boolean {
        val activity = context.findActivity() ?: return false
        return requirement.permissions.any { ActivityCompat.shouldShowRequestPermissionRationale(activity, it) }
    }
}

/** Unwraps [ContextWrapper] layers (themes, Hilt) to reach the hosting activity, or null. */
private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
