package io.github.daredevilq.wistride.core.ui.permission

/**
 * Remembers which requirements the system dialog was already shown for. Needed to tell
 * "never asked" from "don't ask again" (see `resolvePermissionStatus`).
 */
interface PermissionRequestHistory {
    /** True if [markRequested] was called for [id] before. */
    suspend fun wasRequested(id: String): Boolean

    /** Records that the system dialog returned a decision for [id]. */
    suspend fun markRequested(id: String)
}

/**
 * Non-persistent [PermissionRequestHistory] for previews and tests.
 *
 * It lives in main (not the test source set) so feature modules can reuse it in their own
 * previews and tests; the real DataStore-backed implementation arrives in M2.
 */
class InMemoryPermissionRequestHistory(
    initial: Set<String> = emptySet(),
) : PermissionRequestHistory {
    private val requested = initial.toMutableSet()

    override suspend fun wasRequested(id: String): Boolean = id in requested

    override suspend fun markRequested(id: String) {
        requested += id
    }
}
