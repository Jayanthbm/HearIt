package com.jayanth.hearit.core.common.model

/**
 * Identifies active navigation alerts and turn-by-turn guidance notifications (such as Google Maps,
 * Waze, and Android's standard `navigation` category) so lower-priority notification speech yields
 * immediately and resumes afterward.
 */
object NavigationPriorityDetector {
    const val CATEGORY_NAVIGATION = "navigation"

    val NAVIGATION_PACKAGES: Set<String> = setOf(
        "com.google.android.apps.maps",
        "com.waze",
        "com.sygic.aura",
        "net.osmand",
        "net.osmand.plus",
        "com.here.app.maps",
        "com.mapfactor.navigator",
        "com.tomtom.gplay.navapp",
    )

    fun isNavigationNotification(snapshot: NotificationSnapshot): Boolean =
        snapshot.category.equals(CATEGORY_NAVIGATION, ignoreCase = true) ||
            snapshot.packageName in NAVIGATION_PACKAGES
}
