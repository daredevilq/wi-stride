package io.github.daredevilq.wistride.core.model.permission

/** Where a runtime permission currently stands from the app's point of view. */
enum class PermissionStatus {
    /** Every required permission is granted (or the requirement does not apply on this API level). */
    Granted,

    /** The user has never been asked; show a rationale, then the system dialog. */
    NotRequested,

    /** The user declined, but the system dialog can still be shown again. */
    Denied,

    /** The user chose "don't ask again"; only the app's system settings page can grant it now. */
    PermanentlyDenied,
}

/**
 * Derives a [PermissionStatus] from the raw signals Android exposes.
 *
 * [wasRequestedBefore] is required because `shouldShowRequestPermissionRationale` returns false
 * both before the very first request and after the user picked "don't ask again", so the two
 * states are indistinguishable without remembering that a request was already made.
 *
 * @param isGranted whether all permissions are currently granted.
 * @param shouldShowRationale the platform's `shouldShowRequestPermissionRationale` result.
 * @param wasRequestedBefore whether the app has launched the system dialog at least once.
 */
fun resolvePermissionStatus(
    isGranted: Boolean,
    shouldShowRationale: Boolean,
    wasRequestedBefore: Boolean,
): PermissionStatus =
    when {
        isGranted -> PermissionStatus.Granted
        shouldShowRationale -> PermissionStatus.Denied
        wasRequestedBefore -> PermissionStatus.PermanentlyDenied
        else -> PermissionStatus.NotRequested
    }
