package io.github.daredevilq.wistride.core.model.permission

/**
 * One consistent reading of a permission requirement.
 *
 * @property status the resolved [PermissionStatus].
 * @property granted the permissions of the requirement that are granted right now. Consumers that
 * register platform callbacks key on this set, not on [status]: it changes when only some of the
 * permissions are granted in system settings while the status stays the same (e.g. PermanentlyDenied).
 */
data class PermissionSnapshot(
    val status: PermissionStatus,
    val granted: Set<String>,
)
