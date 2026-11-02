package com.jayanth.hearit.service

import com.jayanth.hearit.core.common.model.ResolvedSettings
import com.jayanth.hearit.core.tts.AnnouncementFormat
import com.jayanth.hearit.core.tts.SpeechSettings
import java.util.concurrent.ConcurrentHashMap

/**
 * Supplies resolved settings to the pipeline.
 *
 * **Placeholder until the settings wiring lands.** `:core:datastore` owns the real
 * global/per-app settings; until the service reads it, this returns master-prompt
 * **defaults**: every app disabled (master prompt §5: "All apps disabled by default"),
 * the default announcement format and the default speech settings. Nothing can be
 * announced until that wiring exists — the honest default, not a bug.
 *
 * The interface intentionally mirrors what the real repository provides, so replacing this
 * class with a DataStore-backed implementation is a one-line change in
 * [AnnouncementService].
 *
 * P17 Priority Announce: holds the package names the user added to the Priority Announce
 * list. In-memory for now, like the rest of this placeholder.
 */
class SettingsSource(
    // Kept for the future per-app lookup; reading it now would fake behaviour we do not
    // have, so the default-disabled answer below is deliberate.
    @Suppress("UNUSED_PARAMETER") private val sessionStore: SessionStore,
) {

    private val priorityAnnounceApps = ConcurrentHashMap.newKeySet<String>()

    /** Resolved settings for [packageName]; every app stays disabled until wired. */
    fun forApp(packageName: String): ResolvedSettings = ResolvedSettings(
        appEnabled = false,
        priorityAnnounceListed = isPriorityApp(packageName),
    )

    /** Announcement format (master prompt §7 defaults). */
    fun format(): AnnouncementFormat = AnnouncementFormat()

    /** Speech settings (master prompt §7 defaults: speed/pitch 1.0, engine voice). */
    fun speech(): SpeechSettings = SpeechSettings()

    /** Adds an app to the Priority Announce list (P17). */
    fun addPriorityApp(packageName: String) {
        priorityAnnounceApps.add(packageName)
    }

    /** Removes an app from the Priority Announce list (P17). */
    fun removePriorityApp(packageName: String) {
        priorityAnnounceApps.remove(packageName)
    }

    /** True when the app is in the Priority Announce list (P17). */
    fun isPriorityApp(packageName: String): Boolean = packageName in priorityAnnounceApps
}
