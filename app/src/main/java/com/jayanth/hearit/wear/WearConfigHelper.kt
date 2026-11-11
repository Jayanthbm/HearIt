package com.jayanth.hearit.wear

/**
 * Pure JVM configuration helper enforcing the Wear OS Max-4 Watch Apps invariant (#35).
 */
object WearConfigHelper {
    const val MAX_WATCH_APPS = 4

    fun normalizeWatchApps(packages: Collection<String>): List<String> =
        packages
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .take(MAX_WATCH_APPS)

    fun toggleWatchAppSelection(current: List<String>, packageName: String): List<String> {
        val clean = normalizeWatchApps(current)
        if (packageName.isBlank()) return clean
        return if (packageName in clean) {
            clean - packageName
        } else if (clean.size < MAX_WATCH_APPS) {
            clean + packageName
        } else {
            clean
        }
    }
}
