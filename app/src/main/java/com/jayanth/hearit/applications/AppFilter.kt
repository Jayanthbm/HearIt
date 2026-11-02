package com.jayanth.hearit.applications

import java.util.Locale

/**
 * Pure list operations for the Applications feature (P13).
 *
 * Kept free of Android types so search and selection semantics are JVM-testable: the
 * composables only render the results.
 */
object AppFilter {

    /** Case-insensitive search over label and package name; a blank query keeps everything. */
    fun filter(apps: List<AppEntry>, query: String): List<AppEntry> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return apps
        return apps.filter { app ->
            app.label.contains(trimmed, ignoreCase = true) ||
                app.packageName.contains(trimmed, ignoreCase = true)
        }
    }
}

/**
 * Merges the platform catalog with packages the user has already enabled.
 *
 * An enabled package that the catalog cannot see (for example an app without a launcher
 * icon) must never disappear from the list — it would become impossible to disable.
 * Unknown labels fall back to the package name.
 */
object AppCatalogMerge {

    fun merge(catalog: List<AppEntry>, enabledPackages: Set<String>): List<AppEntry> {
        val byPackage = LinkedHashMap<String, AppEntry>(catalog.size)
        for (entry in catalog) {
            if (entry.packageName.isBlank()) continue
            byPackage[entry.packageName] = entry
        }
        for (packageName in enabledPackages) {
            if (packageName.isBlank() || packageName in byPackage) continue
            byPackage[packageName] = AppEntry(packageName = packageName, label = packageName)
        }
        return byPackage.values.sortedBy { it.label.lowercase(Locale.ROOT) }
    }
}

/**
 * Select-all / deselect-all over the currently visible (filtered) list.
 *
 * Only visible apps change, so "select all" after searching enables the search result, not
 * every installed app — the behaviour the list's copy implies.
 */
object AppSelection {

    fun enable(enabledPackages: Collection<String>, targets: Collection<String>): Set<String> =
        enabledPackages.toSet() + targets.filter { it.isNotBlank() }

    fun disable(enabledPackages: Collection<String>, targets: Collection<String>): Set<String> =
        enabledPackages.toSet() - targets.toSet()
}
