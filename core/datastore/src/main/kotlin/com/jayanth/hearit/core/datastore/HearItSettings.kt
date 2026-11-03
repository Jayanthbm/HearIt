package com.jayanth.hearit.core.datastore

import com.jayanth.hearit.core.common.model.AppEligibilityOverride
import com.jayanth.hearit.core.common.model.AppHistoryOverride
import com.jayanth.hearit.core.common.model.AppPriority
import com.jayanth.hearit.core.common.model.AppQueueOverride
import com.jayanth.hearit.core.common.model.EligibilitySettings
import com.jayanth.hearit.core.common.model.HistorySettings
import com.jayanth.hearit.core.common.model.KnownAudioDevice
import com.jayanth.hearit.core.common.model.QueueSettings
import com.jayanth.hearit.core.common.model.ResolvedSettings
import com.jayanth.hearit.core.common.model.SecuritySettings
import com.jayanth.hearit.core.tts.AnnouncementFormat
import com.jayanth.hearit.core.tts.SpeechSettings

/**
 * Everything the user can configure, in one immutable value (master prompt §5–§12).
 *
 * This is the **global** layer only: per-app overrides live in [appOverrides] as sparse
 * values (null = inherit), and the `resolvedFor`/`queueFor`/`historyFor`/`priorityFor`
 * accessors apply the documented inheritance rules so UI and pipeline never merge by hand.
 *
 * Defaults are the master prompt's shipped defaults: **all apps disabled** (§5), keyword
 * filtering off (§6), phone speaker off (§6), queue defaults from ADR-019, announcement
 * defaults from §7/ADR-018, history full content (§12), audio focus DUCK (P08).
 *
 * Deliberately free of Android types: `:core:datastore` adds DataStore persistence around
 * this value, everything here stays JVM-testable.
 *
 * @property enabledApps Packages the user enabled for announcements. Empty by default.
 * @property priorityAnnounceApps The separate Priority Announce list (master prompt §10),
 *   distinct from [enabledApps].
 * @property knownDevices Audio outputs seen so far, newest first, so the Devices feature can
 *   keep offering a disconnected device (P14). Never contains notification content.
 * @property appOverrides Per-app sparse overrides keyed by package name.
 */
data class HearItSettings(
    val eligibility: EligibilitySettings = EligibilitySettings(),
    val queue: QueueSettings = QueueSettings(),
    val format: AnnouncementFormat = AnnouncementFormat(),
    val speech: SpeechSettings = SpeechSettings(),
    val history: HistorySettings = HistorySettings(),
    val security: SecuritySettings = SecuritySettings(),
    val enabledApps: Set<String> = emptySet(),
    val priorityAnnounceApps: Set<String> = emptySet(),
    val knownDevices: List<KnownAudioDevice> = emptyList(),
    val appOverrides: Map<String, AppOverride> = emptyMap(),
) {

    /** Rules input for [packageName]: global values merged with that app's overrides. */
    fun resolvedFor(packageName: String): ResolvedSettings = ResolvedSettings(
        appEnabled = packageName in enabledApps,
        priorityAnnounceListed = packageName in priorityAnnounceApps || 
            overrideFor(packageName)?.priorityAnnounceOverride?.listed == true,
        eligibility = eligibility.resolveWith(overrideFor(packageName)?.eligibility),
    )

    /** Queue settings for [packageName] (global values plus its grouping-window override). */
    fun queueFor(packageName: String): QueueSettings =
        queue.resolveWith(overrideFor(packageName)?.queue)

    /** History settings for [packageName] (global values plus its overrides). */
    fun historyFor(packageName: String): HistorySettings =
        history.resolveWith(overrideFor(packageName)?.history)

    /** Announcement priority for [packageName]; default [AppPriority.NORMAL] (ADR-019). */
    fun priorityFor(packageName: String): AppPriority =
        overrideFor(packageName)?.priority ?: AppPriority.NORMAL

    /** Priority Announce listing for [packageName] (global list or per-app override). */
    fun priorityAnnounceFor(packageName: String): Boolean =
        packageName in priorityAnnounceApps || overrideFor(packageName)?.priorityAnnounceOverride?.listed == true

    private fun overrideFor(packageName: String): AppOverride? = appOverrides[packageName]
}

/**
 * Per-app override of [EligibilitySettings] for Priority Announce (P19/P20).
 * Absence of a value (null) means the app inherits the global value.
 * overrides never partially mutate another condition or group.
 */
data class PriorityAnnounceOverride(
    val listed: Boolean? = null,
) {
    /** True when this app inherits every global value — shown in the UI as "Inherits global". */
    val inheritsEveryValue: Boolean
        get() = listed == null
}


/**
 * Sparse per-app overrides. Every field defaults to "inherit the global value", so an
 * empty instance means the app inherits everything — exactly what the UI shows as
 * "Inherits global" (master prompt §5, P09 acceptance).
 *
 * @property priority Announcement priority override, or null to inherit Normal.
 */
data class AppOverride(
    val eligibility: AppEligibilityOverride = AppEligibilityOverride(),
    val queue: AppQueueOverride = AppQueueOverride(),
    val history: AppHistoryOverride = AppHistoryOverride(),
    val priority: AppPriority? = null,
    val priorityAnnounceOverride: PriorityAnnounceOverride = PriorityAnnounceOverride(),
) {

    /** True when nothing is overridden for this app. */
    val inheritsEveryValue: Boolean
        get() = eligibility.inheritsEveryValue &&
            queue.inheritsEveryValue &&
            history.inheritsEveryValue &&
            priorityAnnounceOverride.inheritsEveryValue &&
            priority == null
}
