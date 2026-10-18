package io.github.daredevilq.wistride.core.model

/**
 * A value that the platform may not be able to provide. The UI renders [Unavailable] as
 * "not available" together with [UnavailableReason], never as a fake zero.
 */
sealed interface Availability<out T> {
    data class Available<out T>(
        val value: T,
    ) : Availability<T>

    data class Unavailable(
        val reason: UnavailableReason,
    ) : Availability<Nothing>
}

/** Why a value is missing; each case maps to its own explanation (and sometimes an action) in the UI. */
sealed interface UnavailableReason {
    /** The API that reports the value was added in [sdkInt]. */
    data class RequiresApi(
        val sdkInt: Int,
    ) : UnavailableReason

    /** A runtime permission ([permission] is the manifest name) is missing. */
    data class NeedsPermission(
        val permission: String,
    ) : UnavailableReason

    /** The value is redacted while location services are switched off. */
    data object NeedsLocationOn : UnavailableReason

    /** The API exists, but this device/driver/network reports an unknown value. */
    data object NotReportedByDevice : UnavailableReason
}

fun <T> Availability<T>.valueOrNull(): T? = (this as? Availability.Available)?.value

inline fun <T, R> Availability<T>.map(transform: (T) -> R): Availability<R> =
    when (this) {
        is Availability.Available -> Availability.Available(transform(value))
        is Availability.Unavailable -> this
    }

fun <T> T.available(): Availability<T> = Availability.Available(this)

fun UnavailableReason.unavailable(): Availability<Nothing> = Availability.Unavailable(this)
