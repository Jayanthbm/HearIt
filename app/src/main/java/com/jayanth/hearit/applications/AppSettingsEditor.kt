package com.jayanth.hearit.applications

import com.jayanth.hearit.core.common.model.AppCategoryChoice
import com.jayanth.hearit.core.common.model.AppPriority
import com.jayanth.hearit.core.common.model.AudioFocusPolicy
import com.jayanth.hearit.core.common.model.HistoryStorageMode
import com.jayanth.hearit.core.common.model.HistoryUpdateHandling
import com.jayanth.hearit.core.common.model.KeywordFilterSettings
import com.jayanth.hearit.core.common.model.KeywordMatchMode
import com.jayanth.hearit.core.datastore.AppOverride
import com.jayanth.hearit.core.datastore.HearItSettings

/**
 * Pure per-app edits over [HearItSettings] (phase P13).
 *
 * The only thing this object knows is the inheritance contract from ADR-021: `null` means
 * "inherit the global value", and an app whose override is entirely inherited is **removed
 * from the map** so the store stays sparse and the UI's "inherits every value" state is
 * derived from the data rather than tracked separately.
 *
 * Every function is a pure transform, so the ViewModel simply hands it to
 * `SettingsRepository.update`.
 */
object AppSettingsEditor {

    /** Flips one app's announcement switch (master prompt §5: all apps start disabled). */
    fun toggleEnabled(settings: HearItSettings, packageName: String): HearItSettings =
        setEnabled(settings, packageName, enabled = packageName !in settings.enabledApps)

    fun setEnabled(settings: HearItSettings, packageName: String, enabled: Boolean): HearItSettings =
        settings.copy(
            enabledApps = if (enabled) {
                settings.enabledApps + packageName
            } else {
                settings.enabledApps - packageName
            },
        )

    /** "Select all" over the visible list; other enabled apps are untouched. */
    fun enableAll(settings: HearItSettings, packageNames: Collection<String>): HearItSettings =
        settings.copy(enabledApps = AppSelection.enable(settings.enabledApps, packageNames))

    /** "Deselect all" over the visible list; other enabled apps are untouched. */
    fun disableAll(settings: HearItSettings, packageNames: Collection<String>): HearItSettings =
        settings.copy(enabledApps = AppSelection.disable(settings.enabledApps, packageNames))

    /** Sets or clears the announcement priority override (null = inherit Normal). */
    fun setPriority(
        settings: HearItSettings,
        packageName: String,
        priority: AppPriority?,
    ): HearItSettings = updateOverride(settings, packageName) { it.copy(priority = priority) }

    /** Sets or clears the per-app custom spoken name/alias (null = inherit default app label). */
    fun setSpokenName(
        settings: HearItSettings,
        packageName: String,
        spokenName: String?,
    ): HearItSettings = updateOverride(settings, packageName) { override ->
        override.copy(spokenName = spokenName?.trim()?.ifBlank { null })
    }

    /** Assigns or clears the per-app intelligence category (`null` = recognized default). */
    fun setCategoryChoice(
        settings: HearItSettings,
        packageName: String,
        choice: AppCategoryChoice?,
    ): HearItSettings = updateOverride(settings, packageName) { override ->
        override.copy(categoryOverride = choice)
    }

    /** Sets or clears the per-app history storage mode (master prompt §12). */
    fun setHistoryStorageMode(
        settings: HearItSettings,
        packageName: String,
        storageMode: HistoryStorageMode?,
    ): HearItSettings = updateOverride(settings, packageName) { override ->
        override.copy(history = override.history.copy(storageMode = storageMode))
    }

    /** Sets or clears the per-app update handling (master prompt §12). */
    fun setHistoryUpdateHandling(
        settings: HearItSettings,
        packageName: String,
        updateHandling: HistoryUpdateHandling?,
    ): HearItSettings = updateOverride(settings, packageName) { override ->
        override.copy(history = override.history.copy(updateHandling = updateHandling))
    }

    /** Sets or clears the per-app history retention in days (null = inherit global, 0 = never/keep forever). */
    fun setHistoryRetentionDays(
        settings: HearItSettings,
        packageName: String,
        retentionDays: Int?,
    ): HearItSettings = updateOverride(settings, packageName) { override ->
        override.copy(history = override.history.copy(retentionDays = retentionDays))
    }

    /** Sets or clears the per-app Do Not Disturb override (master prompt §6). */
    fun setRespectDnd(
        settings: HearItSettings,
        packageName: String,
        respectDnd: Boolean?,
    ): HearItSettings = updateOverride(settings, packageName) { override ->
        override.copy(eligibility = override.eligibility.copy(respectDnd = respectDnd))
    }

    /** Sets or clears the per-app "announce after a call ends" override (master prompt §6). */
    fun setQueueDuringCall(
        settings: HearItSettings,
        packageName: String,
        queueDuringCall: Boolean?,
    ): HearItSettings = updateOverride(settings, packageName) { override ->
        override.copy(eligibility = override.eligibility.copy(queueDuringCall = queueDuringCall))
    }

    /** Sets or clears the per-app audio-focus policy (master prompt §6). */
    fun setAudioFocus(
        settings: HearItSettings,
        packageName: String,
        audioFocus: AudioFocusPolicy?,
    ): HearItSettings = updateOverride(settings, packageName) { override ->
        override.copy(eligibility = override.eligibility.copy(audioFocus = audioFocus))
    }

    /** Updates or clears custom keyword/phrase/pattern filters for [packageName]. */
    fun setKeywordFilter(
        settings: HearItSettings,
        packageName: String,
        keywordFilter: KeywordFilterSettings?,
    ): HearItSettings = updateOverride(settings, packageName) { override ->
        override.copy(eligibility = override.eligibility.copy(keywordFilter = keywordFilter))
    }

    /** Formats a filter string with its type prefix (word:, phrase:, exact:, pattern:, or sender:). */
    fun formatKeyword(keyword: String, mode: KeywordMatchMode, isSender: Boolean = false): String {
        val trimmed = keyword.trim()
        if (trimmed.isBlank()) return ""
        return when {
            isSender || trimmed.startsWith("sender:", ignoreCase = true) -> {
                val clean = if (trimmed.startsWith("sender:", ignoreCase = true)) trimmed.substring(7).trim() else trimmed
                "sender:$clean"
            }
            trimmed.startsWith("word:", ignoreCase = true) ||
                trimmed.startsWith("phrase:", ignoreCase = true) ||
                trimmed.startsWith("exact:", ignoreCase = true) ||
                trimmed.startsWith("pattern:", ignoreCase = true) -> trimmed
            mode == KeywordMatchMode.WHOLE_WORD -> "word:$trimmed"
            mode == KeywordMatchMode.EXACT -> "exact:$trimmed"
            trimmed.contains("$") || trimmed.contains("*") -> "pattern:$trimmed"
            else -> "phrase:$trimmed"
        }
    }

    /** Appends a keyword, phrase, wildcard pattern, or sender to this app's filter list. */
    fun addKeyword(
        settings: HearItSettings,
        packageName: String,
        keyword: String,
        mode: KeywordMatchMode = KeywordMatchMode.WHOLE_WORD,
        isSender: Boolean = false,
    ): HearItSettings {
        val formatted = formatKeyword(keyword, mode, isSender)
        if (formatted.isBlank()) return settings

        val override = settings.appOverrides[packageName] ?: AppOverride()
        val currentFilter = override.eligibility.keywordFilter ?: settings.eligibility.keywordFilter
        val updatedKeywords = if (formatted in currentFilter.keywords) currentFilter.keywords else currentFilter.keywords + formatted
        val newFilter = currentFilter.copy(enabled = true, keywords = updatedKeywords, mode = mode)
        return setKeywordFilter(settings, packageName, newFilter)
    }

    /** Removes a keyword or pattern from this app's filter list. */
    fun removeKeyword(
        settings: HearItSettings,
        packageName: String,
        keyword: String,
    ): HearItSettings {
        val target = keyword.trim()
        val override = settings.appOverrides[packageName] ?: AppOverride()
        val currentFilter = override.eligibility.keywordFilter ?: settings.eligibility.keywordFilter
        val updatedKeywords = currentFilter.keywords.filterNot { raw ->
            raw.equals(target, ignoreCase = true) ||
                parseFilterItem(raw).text.equals(target, ignoreCase = true)
        }
        val newFilter = if (updatedKeywords.isEmpty()) {
            null // inherit global when all custom keywords removed
        } else {
            currentFilter.copy(keywords = updatedKeywords)
        }
        return setKeywordFilter(settings, packageName, newFilter)
    }

    data class ParsedFilterItem(
        val raw: String,
        val type: String, // "Word", "Phrase", "Pattern", "Exact"
        val text: String,
    )

    fun parseFilterItem(raw: String): ParsedFilterItem {
        val trimmed = raw.trim()
        return when {
            trimmed.startsWith("sender:", ignoreCase = true) ->
                ParsedFilterItem(raw, "Sender", trimmed.substring(7).trim())
            trimmed.startsWith("word:", ignoreCase = true) ->
                ParsedFilterItem(raw, "Word", trimmed.substring(5).trim())
            trimmed.startsWith("phrase:", ignoreCase = true) ->
                ParsedFilterItem(raw, "Phrase", trimmed.substring(7).trim())
            trimmed.startsWith("pattern:", ignoreCase = true) ->
                ParsedFilterItem(raw, "Pattern", trimmed.substring(8).trim())
            trimmed.startsWith("exact:", ignoreCase = true) ->
                ParsedFilterItem(raw, "Exact", trimmed.substring(6).trim())
            trimmed.contains("$") || trimmed.contains("*") ->
                ParsedFilterItem(raw, "Pattern", trimmed)
            !trimmed.contains(" ") ->
                ParsedFilterItem(raw, "Word", trimmed)
            else ->
                ParsedFilterItem(raw, "Phrase", trimmed)
        }
    }

    /** Changes the keyword match mode (WHOLE_WORD, PHRASE, EXACT). */
    fun setKeywordMatchMode(
        settings: HearItSettings,
        packageName: String,
        mode: KeywordMatchMode,
    ): HearItSettings {
        val override = settings.appOverrides[packageName] ?: AppOverride()
        val currentFilter = override.eligibility.keywordFilter ?: settings.eligibility.keywordFilter
        val newFilter = currentFilter.copy(mode = mode)
        return setKeywordFilter(settings, packageName, newFilter)
    }

    /** Enables or disables keyword filtering specifically for this app. */
    fun toggleKeywordFilterEnabled(
        settings: HearItSettings,
        packageName: String,
        enabled: Boolean,
    ): HearItSettings {
        val override = settings.appOverrides[packageName] ?: AppOverride()
        val currentFilter = override.eligibility.keywordFilter ?: settings.eligibility.keywordFilter
        val newFilter = currentFilter.copy(enabled = enabled)
        return setKeywordFilter(settings, packageName, newFilter)
    }

    /**
     * Clears every override for one app, returning it to pure inheritance. The
     * announcement switch is a separate user decision and is deliberately untouched.
     */
    fun resetToGlobal(settings: HearItSettings, packageName: String): HearItSettings =
        settings.copy(appOverrides = settings.appOverrides - packageName)

    /** Every per-app value resolved for [packageName], with its inheritance state. */
    fun settingsFor(
        settings: HearItSettings,
        packageName: String,
        label: String,
    ): AppSettingsUiState {
        val override = settings.appOverrides[packageName] ?: AppOverride()
        return AppSettingsUiState(
            packageName = packageName,
            label = label.ifBlank { packageName },
            enabled = packageName in settings.enabledApps,
            priority = Inherited(override.priority, AppPriority.NORMAL),
            historyStorageMode = Inherited(override.history.storageMode, settings.history.storageMode),
            historyUpdateHandling =
                Inherited(override.history.updateHandling, settings.history.updateHandling),
            historyRetention =
                Inherited(override.history.retentionDays, settings.history.retentionDays),
            respectDnd = Inherited(override.eligibility.respectDnd, settings.eligibility.respectDnd),
            queueDuringCall =
                Inherited(override.eligibility.queueDuringCall, settings.eligibility.queueDuringCall),
            audioFocus = Inherited(override.eligibility.audioFocus, settings.eligibility.audioFocus),
            keywordFilter = Inherited(override.eligibility.keywordFilter, settings.eligibility.keywordFilter),
            enabledAppCount = settings.enabledApps.size,
            hasOverrides = !override.inheritsEveryValue,
            spokenName = override.spokenName,
            category = settings.categoryFor(packageName),
            categoryOverride = override.categoryOverride,
        )
    }

    /**
     * Applies [transform] to one app's sparse override, dropping the entry again as soon as
     * everything is inherited so "inherits global" stays truthful.
     */
    private fun updateOverride(
        settings: HearItSettings,
        packageName: String,
        transform: (AppOverride) -> AppOverride,
    ): HearItSettings {
        val current = settings.appOverrides[packageName] ?: AppOverride()
        val next = transform(current)
        val overrides = if (next.inheritsEveryValue) {
            settings.appOverrides - packageName
        } else {
            settings.appOverrides + (packageName to next)
        }
        return settings.copy(appOverrides = overrides)
    }
}
