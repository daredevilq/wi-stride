package io.github.daredevilq.wistride.core.ui.permission

import androidx.annotation.StringRes

/**
 * A group of runtime permissions that are requested together for one feature.
 *
 * @property id stable key used to remember that the request was already made.
 * @property permissions manifest permission names; all must be granted.
 * @property titleRes short heading of the in-app rationale.
 * @property rationaleRes explanation shown before the system dialog.
 * @property minSdk first API level on which the permission is needed.
 * @property maxSdk last API level on which the permission is needed.
 */
data class PermissionRequirement(
    val id: String,
    val permissions: List<String>,
    @StringRes val titleRes: Int,
    @StringRes val rationaleRes: Int,
    val minSdk: Int = 1,
    val maxSdk: Int = Int.MAX_VALUE,
) {
    /** Whether the requirement matters on [sdkInt]; if not, it counts as granted. */
    fun isApplicable(sdkInt: Int): Boolean = sdkInt in minSdk..maxSdk
}
