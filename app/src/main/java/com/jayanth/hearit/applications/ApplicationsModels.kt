package com.jayanth.hearit.applications

import com.jayanth.hearit.core.common.model.AppCategory
import com.jayanth.hearit.core.common.model.AppCategoryChoice
import com.jayanth.hearit.core.common.model.AppPriority
import com.jayanth.hearit.core.common.model.AudioFocusPolicy
import com.jayanth.hearit.core.common.model.HistoryStorageMode
import com.jayanth.hearit.core.common.model.HistoryUpdateHandling
import com.jayanth.hearit.core.common.model.KeywordFilterSettings
import com.jayanth.hearit.core.database.HistoryEntry
import com.jayanth.hearit.core.tts.TtsVoice

/** One application the user can enable (master prompt §5, phase P13). */
data class AppEntry(
    val packageName: String,
    val label: String,
    val isSystemApp: Boolean = false,
)

/**
 * One settings value that may be inherited from the global default (master prompt §5:
 * "app settings must clearly show when a value inherits the global value").
 *
 * @property override The per-app value, or null when the app inherits.
 * @property global The value that applies while [override] is null.
 */
data class Inherited<T>(
    val override: T?,
    val global: T,
) {

    /** True while this app inherits the global value for this setting. */
    val isInherited: Boolean get() = override == null

    /** The value that actually applies (per-app override, else the global value). */
    val effective: T get() = override ?: global
}

/**
 * List state of the Applications feature.
 *
 * @property loading True until the installed-app catalog has been read.
 * @property catalogUnavailable True when the catalog query failed; the list says so
 *   instead of looking empty.
 * @property query Current search text.
 * @property apps Every known app (catalog plus previously enabled packages), sorted.
 * @property visibleApps [apps] filtered by [query] — the rows select-all/deselect-all act on.
 * @property enabledApps Packages enabled for announcements (all disabled by default).
 * @property openPackage Package whose settings screen is open, or null for the list.
 * @property settings Settings of [openPackage], or null while the list is shown.
 */
data class ApplicationsUiState(
    val loading: Boolean = true,
    val catalogUnavailable: Boolean = false,
    val query: String = "",
    val apps: List<AppEntry> = emptyList(),
    val visibleApps: List<AppEntry> = emptyList(),
    val enabledApps: Set<String> = emptySet(),
    val priorityAnnounceApps: Set<String> = emptySet(),
    val customizedApps: Set<String> = emptySet(),
    val appCategories: Map<String, AppCategory?> = emptyMap(),
    val availableVoices: List<TtsVoice> = emptyList(),
    val isSelectionMode: Boolean = false,
    val selectedPackages: Set<String> = emptySet(),
    val openPackage: String? = null,
    val settings: AppSettingsUiState? = null,
)

/**
 * Per-app settings with explicit inheritance, ready to render.
 *
 * Every value is an [Inherited], so the UI can show "Inherit global (value)" next to each
 * explicit choice without computing resolution itself (global standards: no policy in
 * composables).
 *
 * @property hasOverrides True when this app overrides at least one value; drives the
 *   "Reset to global values" affordance.
 */
data class AppSettingsUiState(
    val packageName: String = "",
    val label: String = "",
    val enabled: Boolean = false,
    val priorityAnnounceListed: Boolean = false,
    val priority: Inherited<AppPriority> = Inherited(null, AppPriority.NORMAL),
    val speechRate: Inherited<Float> = Inherited(null, 1.0f),
    val speechPitch: Inherited<Float> = Inherited(null, 1.0f),
    val speechVoiceId: Inherited<String?> = Inherited(null, null),
    val autoDismiss: Inherited<Boolean> = Inherited(null, true),
    val availableVoices: List<TtsVoice> = emptyList(),
    val historyStorageMode: Inherited<HistoryStorageMode> =
        Inherited(null, HistoryStorageMode.FULL_CONTENT),
    val historyUpdateHandling: Inherited<HistoryUpdateHandling> =
        Inherited(null, HistoryUpdateHandling.UPDATE_ENTRY),
    val historyRetention: Inherited<Int?> = Inherited(null, null),
    val respectDnd: Inherited<Boolean> = Inherited(null, true),
    val queueDuringCall: Inherited<Boolean> = Inherited(null, false),
    val audioFocus: Inherited<AudioFocusPolicy> = Inherited(null, AudioFocusPolicy.DUCK),
    val keywordFilter: Inherited<KeywordFilterSettings> =
        Inherited(null, KeywordFilterSettings()),
    val recentHistory: List<HistoryEntry> = emptyList(),
    val historyLoading: Boolean = false,
    val playingHistoryId: Long? = null,
    val enabledAppCount: Int = 0,
    val hasOverrides: Boolean = false,
    val spokenName: String? = null,
    val category: AppCategory? = null,
    val categoryOverride: AppCategoryChoice? = null,
)
