package com.jayanth.hearit.service

import com.jayanth.hearit.core.common.model.AppPriority
import com.jayanth.hearit.core.common.model.AppQueueOverride
import com.jayanth.hearit.core.common.model.QueueSettings
import com.jayanth.hearit.core.common.model.ResolvedSettings
import com.jayanth.hearit.core.datastore.HearItSettings
import com.jayanth.hearit.core.datastore.SettingsRepository
import com.jayanth.hearit.core.tts.AnnouncementFormat
import com.jayanth.hearit.core.tts.SmartContentCleaner
import com.jayanth.hearit.core.tts.SpeechSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * Supplies the persisted settings to the announcement pipeline (L2).
 *
 * The pipeline is deliberately synchronous — it runs inline in the service's main-thread
 * tick loop — while [SettingsRepository] is `suspend`/`Flow` based (DataStore). This class
 * is the seam between the two: it mirrors the repository flow into an immutable snapshot
 * that callers read synchronously.
 *
 * Until the first emission arrives the snapshot holds [HearItSettings]'s defaults, which
 * **disable every app** (master prompt §5). A missed or delayed read therefore produces no
 * announcement — the safe direction — instead of announcing something the user never
 * enabled.
 *
 * All merge/inheritance logic lives in [HearItSettings]'s typed accessors (ADR-021); this
 * class never merges a value by hand.
 */
class SettingsSource(private val repository: SettingsRepository) {

    private val state = MutableStateFlow(HearItSettings())

    /** Latest known settings; safe to read from the main thread. */
    val settings: HearItSettings get() = state.value

    /** Starts mirroring [SettingsRepository.settings] into [settings] for [scope]'s lifetime. */
    fun observe(scope: CoroutineScope): Job = scope.launch {
        repository.settings.collect { incoming ->
            state.value = incoming
            syncCleanerFromSettings(incoming)
        }
    }

    /** One-shot read, for callers that need the stored value before the flow emits. */
    suspend fun reload() {
        val loaded = repository.current()
        state.value = loaded
        syncCleanerFromSettings(loaded)
    }

    private fun syncCleanerFromSettings(settings: HearItSettings) {
        val aliases = settings.appOverrides.mapNotNull { (pkg, override) ->
            override.spokenName?.takeIf { it.isNotBlank() }?.let { pkg to it }
        }.toMap()
        val categories = settings.appOverrides.mapNotNull { (pkg, override) ->
            override.categoryOverride?.let { pkg to it }
        }.toMap()
        SmartContentCleaner.replaceAppAliases(aliases)
        SmartContentCleaner.replaceAppCategories(categories)
    }

    /** Rules input for [packageName]: global values merged with that app's overrides. */
    fun forApp(packageName: String): ResolvedSettings = settings.resolvedFor(packageName)

    /** Announcement priority for [packageName] (ADR-019: default Normal). */
    fun priorityFor(packageName: String): AppPriority = settings.priorityFor(packageName)

    /** Announcement format (master prompt §7). */
    fun format(): AnnouncementFormat = settings.format

    /** Speech settings (master prompt §7: speed, pitch, voice). */
    fun speech(): SpeechSettings = settings.speech

    /** Global queue/scheduling settings. */
    fun queueSettings(): QueueSettings = settings.queue

    /** Per-app queue overrides, keyed by package name. */
    fun queueOverrides(): Map<String, AppQueueOverride> =
        settings.appOverrides.mapValues { (_, override) -> override.queue }

    /** True when [packageName] is in the Priority Announce list (P17). */
    fun isPriorityApp(packageName: String): Boolean = settings.priorityAnnounceFor(packageName)

    /** Adds or removes [packageName] from the Priority Announce list (P17). */
    suspend fun setPriorityApp(packageName: String, listed: Boolean) {
        repository.update { current ->
            if (listed) {
                current.copy(priorityAnnounceApps = current.priorityAnnounceApps + packageName)
            } else {
                current.copy(priorityAnnounceApps = current.priorityAnnounceApps - packageName)
            }
        }
    }
}
