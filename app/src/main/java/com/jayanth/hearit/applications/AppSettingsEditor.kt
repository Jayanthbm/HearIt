package com.jayanth.hearit.applications

import com.jayanth.hearit.core.common.model.AppPriority
import com.jayanth.hearit.core.common.model.AudioFocusPolicy
import com.jayanth.hearit.core.common.model.HistoryStorageMode
import com.jayanth.hearit.core.common.model.HistoryUpdateHandling
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
            respectDnd = Inherited(override.eligibility.respectDnd, settings.eligibility.respectDnd),
            queueDuringCall =
                Inherited(override.eligibility.queueDuringCall, settings.eligibility.queueDuringCall),
            audioFocus = Inherited(override.eligibility.audioFocus, settings.eligibility.audioFocus),
            enabledAppCount = settings.enabledApps.size,
            hasOverrides = !override.inheritsEveryValue,
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
