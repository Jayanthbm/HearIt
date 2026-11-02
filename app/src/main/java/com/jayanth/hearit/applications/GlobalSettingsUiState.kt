package com.jayanth.hearit.applications

import com.jayanth.hearit.core.common.model.ScreenCondition
import com.jayanth.hearit.core.common.model.LockCondition
import com.jayanth.hearit.core.common.model.EligibilitySettings
import com.jayanth.hearit.core.common.model.TextLengthSettings
import com.jayanth.hearit.core.common.model.KeywordFilterSettings
import com.jayanth.hearit.core.common.model.PrivacySettings
import com.jayanth.hearit.core.common.model.AudioOutputSettings
import com.jayanth.hearit.core.common.model.AudioOutputKind

/** UiState for global (not per-app) settings configuration (phase P15). */
data class GlobalSettingsUiState(

    /** Screen condition: ON (announce only on), OFF (announce only off), or BOTH. Default OFF. */
    val screenCondition: ScreenCondition = ScreenCondition.OFF,

    /** Lock condition: LOCKED (announce only when locked), UNLOCKED (announce only when unlocked), or BOTH. Default LOCKED. */
    val lockCondition: LockCondition = LockCondition.LOCKED,

    /** Text length limit configuration. null = no limit. */
    val textLength: TextLengthSettings = TextLengthSettings(),

    /** Keyword filter configuration. */
    val keywordFilter: KeywordFilterSettings = KeywordFilterSettings(),

    /** Privacy rules configuration. */
    val privacy: PrivacySettings = PrivacySettings(),

    /** Audio output allowlist configuration. */
    val audioOutput: AudioOutputSettings = AudioOutputSettings(),

    /** Whether the global settings form has any non-inherited overrides. */
    val hasOverrides: Boolean
        get() = screenCondition != ScreenCondition.OFF ||
            lockCondition != LockCondition.LOCKED ||
            textLength != TextLengthSettings() ||
            keywordFilter != KeywordFilterSettings() ||
            privacy != PrivacySettings() ||
            audioOutput != AudioOutputSettings()
)