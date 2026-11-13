package com.jayanth.hearit.applications

import com.jayanth.hearit.core.common.model.AudioOutputSettings
import com.jayanth.hearit.core.common.model.EligibilitySettings
import com.jayanth.hearit.core.common.model.KeywordFilterSettings
import com.jayanth.hearit.core.common.model.LockCondition
import com.jayanth.hearit.core.common.model.PrivacySettings
import com.jayanth.hearit.core.common.model.ScreenCondition
import com.jayanth.hearit.core.common.model.TextLengthSettings
import com.jayanth.hearit.core.datastore.HearItSettings
import com.jayanth.hearit.core.tts.AnnouncementFormat

/**
 * UiState for the global rules form (#2, #23, #29, #30).
 */
data class GlobalSettingsUiState(
    val screenCondition: ScreenCondition = ScreenCondition.OFF,
    val lockCondition: LockCondition = LockCondition.LOCKED,
    val textLength: TextLengthSettings = TextLengthSettings(),
    val keywordFilter: KeywordFilterSettings = KeywordFilterSettings(),
    val privacy: PrivacySettings = PrivacySettings(),
    val audioOutput: AudioOutputSettings = AudioOutputSettings(),
    val eligibility: EligibilitySettings = EligibilitySettings(),
    val format: AnnouncementFormat = AnnouncementFormat(),
) {

    /** True when at least one group differs from the documented defaults. */
    val hasOverrides: Boolean
        get() = eligibility != EligibilitySettings() ||
            screenCondition != ScreenCondition.OFF ||
            lockCondition != LockCondition.LOCKED ||
            privacy != PrivacySettings() ||
            format != AnnouncementFormat()

    companion object {

        /** Builds the form state from persisted settings. */
        fun from(settings: HearItSettings): GlobalSettingsUiState = GlobalSettingsUiState(
            screenCondition = settings.eligibility.screenCondition,
            lockCondition = settings.eligibility.lockCondition,
            textLength = settings.eligibility.textLength,
            keywordFilter = settings.eligibility.keywordFilter,
            privacy = settings.eligibility.privacy,
            audioOutput = settings.eligibility.audioOutput,
            eligibility = settings.eligibility,
            format = settings.format,
        )
    }
}
