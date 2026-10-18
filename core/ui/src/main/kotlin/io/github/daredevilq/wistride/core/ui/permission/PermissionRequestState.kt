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
import io.github.daredevilq.wistride.core.model.permission.PermissionStatus
import io.github.daredevilq.wistride.core.model.permission.resolvePermissionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Observable status of one [PermissionRequirement] plus the actions to improve it. */
@Stable
interface PermissionRequestState {
    /** Current status; updated after the system dialog and when returning from settings. */
    val status: PermissionStatus

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

    // Synchronous first guess so an already granted permission never flashes the rationale card;
    // the request history (suspending) refines NotRequested vs PermanentlyDenied in refresh().
    override var status by mutableStateOf(
        resolvePermissionStatus(isGranted(), shouldShowRationale(), wasRequestedBefore = false),
    )
        private set

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
        status = resolvePermissionStatus(isGranted(), shouldShowRationale(), history.wasRequested(requirement.id))
    }

    private fun isGranted(): Boolean =
        !requirement.isApplicable(Build.VERSION.SDK_INT) ||
            requirement.permissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }

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
