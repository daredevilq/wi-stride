package io.github.daredevilq.wistride.core.model.permission

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
 * Lives in main (not a test source set) so every module can use it in previews and tests; the app
 * binds the DataStore-backed implementation from `core:datastore`.
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
