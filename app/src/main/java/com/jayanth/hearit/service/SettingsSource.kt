package com.jayanth.hearit.service

import com.jayanth.hearit.core.common.model.ResolvedSettings
import com.jayanth.hearit.core.tts.AnnouncementFormat
import com.jayanth.hearit.core.tts.SpeechSettings

/**
 * Supplies resolved settings to the pipeline.
 *
 * **Placeholder until P09/P13.** `:core:datastore` (P09) and app selection (P13) own the
 * real global/per-app settings; until then this returns master-prompt **defaults**:
 * every app disabled (master prompt §5: "All apps disabled by default"), default
 * announcement format and default speech settings. Nothing can be announced until the
 * settings layer and app selection land — the honest default, not a bug.
 *
 * The interface intentionally mirrors what the real repository will provide, so replacing
 * this class with the DataStore-backed implementation is a one-line change in
 * [AnnouncementService].
 */
class SettingsSource(
    // Kept for the future per-app lookup (P13); reading it now would fake behaviour we do
    // not have, so the default-disabled answer below is deliberate.
    @Suppress("UNUSED_PARAMETER") private val sessionStore: SessionStore,
) {

    /** Resolved settings for [packageName]; all apps disabled until P13 selects them. */
    fun forApp(packageName: String): ResolvedSettings = ResolvedSettings(appEnabled = false)

    /** Announcement format (master prompt §7 defaults). */
    fun format(): AnnouncementFormat = AnnouncementFormat()

    /** Speech settings (master prompt §7 defaults: speed/pitch 1.0, engine voice). */
    fun speech(): SpeechSettings = SpeechSettings()
}
