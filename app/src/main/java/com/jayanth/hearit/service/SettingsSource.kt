package com.jayanth.hearit.service

import com.jayanth.hearit.core.common.model.ResolvedSettings
import com.jayanth.hearit.core.common.model.ScreenCondition
import com.jayanth.hearit.core.tts.AnnouncementFormat
import com.jayanth.hearit.core.tts.SpeechSettings
import java.util.concurrent.ConcurrentHashMap

/**
 * Supplies resolved settings to the pipeline.
 *
 * **Placeholder until P09/P13/P17.** `:core:datastore` (P09) and app selection (P13) own the
 * real global/per-app settings; until then this returns master-prompt **defaults**:
 * every app disabled (master prompt §5: "All apps disabled by default"), default
 * announcement format and default speech settings. Nothing can be announced until the
 * settings layer and app selection land — the honest default, not a bug.
 *
 * The interface intentionally mirrors what the real repository will provide, so replacing
 * this class with the DataStore-backed implementation is a one-line change in
 * [AnnouncementService].
 *
 * P17 Priority Announce: holds a mutable set of package names that the user has added
 * to the Priority Announce list. In production this would be DataStore-backed.
 */
class SettingsSource(
    // Kept for the future per-app lookup (P13); reading it now would fake behaviour we do
    // not have, so the default-disabled answer below is deliberate.
    @Suppress("UNUSED_PARAMETER") private val sessionStore: SessionStore,
    // P17 Priority Announce app list (mutable in-memory; real impl: DataStore)
    private val priorityAnnounceAppList = ConcurrentHashMap<String, Boolean>(),
) {

    /** Resolved settings for [packageName]; all apps disabled until P13 selects them. */
    fun forApp(packageName: String): ResolvedSettings = ResolvedSettings(
        appEnabled = priorityAnnounceAppList[packageName] ?: false,
        priorityAnnounceListed = true
    )

    /** Announcement format (master prompt §7 defaults). */
    fun format(): AnnouncementFormat = AnnouncementFormat()

    /** Speech settings (master prompt §7 defaults: speed/pitch 1.0, engine voice). */
    fun speech(): SpeechSettings = SpeechSettings()

    /** Adds an app to the Priority Announce list (P17). */
    fun addPriorityApp(packageName: String) {
        priorityAnnounceAppList[packageName] = true
    }

    /** Removes an app from the Priority Announce list (P17). */
    fun removePriorityApp(packageName: String) {
        priorityAnnounceAppList.remove(packageName)
    }

    /** Checks if an app is in the Priority Announce list (P17). */
    fun isPriorityApp(packageName: String): Boolean = priorityAnnounceAppList[packageName] ?: false
}
