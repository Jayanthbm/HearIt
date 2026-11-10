package com.jayanth.hearit.applications

import com.jayanth.hearit.core.common.model.AudioOutputSettings
import com.jayanth.hearit.core.common.model.KeywordFilterSettings
import com.jayanth.hearit.core.common.model.LockCondition
import com.jayanth.hearit.core.common.model.PrivacySettings
import com.jayanth.hearit.core.common.model.ScreenCondition
import com.jayanth.hearit.core.common.model.TextLengthSettings
import com.jayanth.hearit.core.datastore.HearItSettings

/**
 * UiState for the global (not per-app) rules form (phase P15).
 *
 * A plain projection of the settings the screen edits; it holds no Compose or Android
 * types so it can be asserted in JVM tests.
 */
data class GlobalSettingsUiState(
    val screenCondition: ScreenCondition = ScreenCondition.OFF,
    val lockCondition: LockCondition = LockCondition.LOCKED,
    val textLength: TextLengthSettings = TextLengthSettings(),
    val keywordFilter: KeywordFilterSettings = KeywordFilterSettings(),
    val privacy: PrivacySettings = PrivacySettings(),
    val audioOutput: AudioOutputSettings = AudioOutputSettings(),
) {

    /** True when at least one group differs from the documented defaults. */
    val hasOverrides: Boolean
        get() = screenCondition != ScreenCondition.OFF ||
            lockCondition != LockCondition.LOCKED ||
            privacy != PrivacySettings()

    companion object {

        /** Builds the form state from persisted settings. */
        fun from(settings: HearItSettings): GlobalSettingsUiState = GlobalSettingsUiState(
            screenCondition = settings.eligibility.screenCondition,
            lockCondition = settings.eligibility.lockCondition,
            textLength = settings.eligibility.textLength,
            keywordFilter = settings.eligibility.keywordFilter,
            privacy = settings.eligibility.privacy,
            audioOutput = settings.eligibility.audioOutput,
        )
    }
}
