package io.github.daredevilq.wistride.domain.network

/** Result of [validateTarget]. */
sealed interface TargetValidation {
    /** [host] is normalized (trimmed, brackets and trailing dot stripped, lowercased); [port] null = not given. */
    data class Valid(
        val host: String,
        val port: Int?,
    ) : TargetValidation

    data class Invalid(
        val error: TargetInputError,
    ) : TargetValidation
}

/** What is wrong with the user's input; each case has its own message in the UI. */
enum class TargetInputError { EmptyHost, InvalidHost, InvalidPort }

private const val MAX_PORT = 65_535
private const val MAX_HOST_LENGTH = 253
private const val MAX_LABEL_LENGTH = 63
private const val IPV4_PARTS = 4
private const val MAX_OCTET = 255
private const val MAX_OCTET_DIGITS = 3
private const val IPV6_GROUPS = 8
private const val MAX_IPV6_GROUP_DIGITS = 4

/**
 * Validates the Add-target form without touching the network: the check is purely syntactic, never
 * `InetAddress` (which can start a DNS lookup on the calling thread).
 *
 * The host may be an IPv4 dotted quad, an IPv6 literal (optionally in `[]`, optionally with a
 * `%zone`) or an RFC 1123 host name (labels of 1–63 `[a-z0-9-]`, no leading/trailing `-`, at most
 * 253 characters, one optional trailing dot). A blank [portInput] means "no port".
 */
fun validateTarget(
    hostInput: String,
    portInput: String,
): TargetValidation {
    val raw = hostInput.trim()
    if (raw.isEmpty()) return TargetValidation.Invalid(TargetInputError.EmptyHost)
    val host = normalizeHost(raw) ?: return TargetValidation.Invalid(TargetInputError.InvalidHost)
    val portText = portInput.trim()
    val port =
        if (portText.isEmpty()) {
            null
        } else {
            parsePort(portText) ?: return TargetValidation.Invalid(TargetInputError.InvalidPort)
        }
    return TargetValidation.Valid(host, port)
}

// toIntOrNull accepts a sign; ports are plain digits only. Over-long input would overflow Int.
private fun parsePort(text: String): Int? =
    if (text.all(::isDigit)) text.toIntOrNull()?.takeIf { it in 1..MAX_PORT } else null

private fun normalizeHost(raw: String): String? =
    when {
        raw.startsWith("[") -> if (raw.endsWith("]")) normalizeIpv6(raw.drop(1).dropLast(1)) else null
        ':' in raw -> normalizeIpv6(raw)
        else -> normalizeIpv4OrName(raw)
    }

private fun normalizeIpv4OrName(raw: String): String? {
    val name = raw.removeSuffix(".").lowercase()
    if (name.isEmpty() || name.length > MAX_HOST_LENGTH) return null
    val labels = name.split('.')
    if (!labels.all(::isValidLabel)) return null
    // A numeric last label makes resolvers parse the whole string as an IPv4 literal, so it must be one.
    if (labels.last().all(::isDigit) && !isIpv4(labels)) return null
    return name
}

private fun isValidLabel(label: String): Boolean =
    label.length in 1..MAX_LABEL_LENGTH &&
        !label.startsWith('-') &&
        !label.endsWith('-') &&
        label.all { it in 'a'..'z' || isDigit(it) || it == '-' }

private fun isIpv4(parts: List<String>): Boolean =
    parts.size == IPV4_PARTS &&
        parts.all { it.length in 1..MAX_OCTET_DIGITS && it.all(::isDigit) && it.toInt() <= MAX_OCTET }

private fun normalizeIpv6(text: String): String? {
    val zoneAt = text.indexOf('%')
    val address = if (zoneAt >= 0) text.substring(0, zoneAt) else text
    val zone = if (zoneAt >= 0) text.substring(zoneAt) else ""
    // A bare "%" or a zone with no address is meaningless; zone ids themselves are opaque.
    if (zone.length == 1 || !isIpv6(address.lowercase())) return null
    return address.lowercase() + zone
}

/** Counts 16-bit groups (an IPv4 suffix counts as two) and allows at most one `::`. */
private fun isIpv6(address: String): Boolean {
    val compressedAt = address.indexOf("::")
    if (compressedAt >= 0 && address.indexOf("::", compressedAt + 1) >= 0) return false
    val head = if (compressedAt >= 0) address.substring(0, compressedAt) else address
    val tail = if (compressedAt >= 0) address.substring(compressedAt + 2) else ""
    val groups = (splitGroups(head) ?: return false) + (splitGroups(tail) ?: return false)
    // An embedded IPv4 address may only end the literal ("::ffff:1.2.3.4", never "1.2.3.4::").
    val embedded = groups.lastOrNull()?.takeIf { '.' in it && address.endsWith(it) }
    val hexGroups = if (embedded != null) groups.dropLast(1) else groups
    if (!hexGroups.all(::isHexGroup)) return false
    if (embedded != null && !isIpv4(embedded.split('.'))) return false
    val count = hexGroups.size + if (embedded != null) 2 else 0
    // "::" stands for at least one group of zeros.
    return if (compressedAt >= 0) count < IPV6_GROUPS else count == IPV6_GROUPS
}

private fun splitGroups(part: String): List<String>? =
    if (part.isEmpty()) emptyList() else part.split(':').takeIf { groups -> groups.none { it.isEmpty() } }

private fun isHexGroup(group: String): Boolean =
    group.length in 1..MAX_IPV6_GROUP_DIGITS && group.all { isDigit(it) || it in 'a'..'f' }

private fun isDigit(c: Char): Boolean = c in '0'..'9'
