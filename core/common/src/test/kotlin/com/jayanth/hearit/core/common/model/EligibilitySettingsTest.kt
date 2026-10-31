package com.jayanth.hearit.core.common.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Inheritance behaviour of per-app overrides: a null override field inherits the global
 * value and must never leak into the other condition (master prompt §5/§6).
 */
class EligibilitySettingsTest {

    private val global = EligibilitySettings(
        screenCondition = ScreenCondition.OFF,
        lockCondition = LockCondition.LOCKED,
    )

    @Test
    fun resolveWith_nullOverride_inheritsAllGlobalValues() {
        assertEquals(global, global.resolveWith(null))
    }

    @Test
    fun resolveWith_noFieldsSet_inheritsEveryValue() {
        val override = AppEligibilityOverride()
        assertTrue(override.inheritsEveryValue)
        assertEquals(global, global.resolveWith(override))
    }

    @Test
    fun resolveWith_onlyScreenOverridden_keepsGlobalLockCondition() {
        val resolved = global.resolveWith(AppEligibilityOverride(screenCondition = ScreenCondition.BOTH))
        assertEquals(ScreenCondition.BOTH, resolved.screenCondition)
        assertEquals(LockCondition.LOCKED, resolved.lockCondition)
    }

    @Test
    fun resolveWith_onlyLockOverridden_keepsGlobalScreenCondition() {
        val resolved = global.resolveWith(AppEligibilityOverride(lockCondition = LockCondition.UNLOCKED))
        assertEquals(ScreenCondition.OFF, resolved.screenCondition)
        assertEquals(LockCondition.UNLOCKED, resolved.lockCondition)
    }

    @Test
    fun resolveWith_bothFieldsOverridden_replacesBothValues() {
        val resolved = global.resolveWith(
            AppEligibilityOverride(
                screenCondition = ScreenCondition.ON,
                lockCondition = LockCondition.BOTH,
            ),
        )
        assertEquals(ScreenCondition.ON, resolved.screenCondition)
        assertEquals(LockCondition.BOTH, resolved.lockCondition)
    }

    @Test
    fun defaults_matchMasterPrompt_screenOffAndLocked() {
        assertEquals(ScreenCondition.OFF, EligibilitySettings().screenCondition)
        assertEquals(LockCondition.LOCKED, EligibilitySettings().lockCondition)
    }

    // --- P02 groups: text length, keywords, privacy -------------------------------------

    @Test
    fun defaults_matchMasterPrompt_keywordsDisabledAndNoLengthLimit() {
        val defaults = EligibilitySettings()
        assertFalse(defaults.keywordFilter.enabled)
        assertNull(defaults.textLength.maxLengthChars)
        assertEquals(LengthScope.MESSAGE, defaults.textLength.scope)
        // ADR-015: high-precision privacy rules ship on, low-precision ones off.
        assertEquals(true, defaults.privacy.skipOtp)
        assertEquals(true, defaults.privacy.skipPassword)
        assertFalse(defaults.privacy.skipBanking)
        assertFalse(defaults.privacy.skipPayment)
        assertFalse(defaults.privacy.excludeApp)
        assertFalse(defaults.privacy.hideContent)
        assertEquals(emptyList<String>(), defaults.privacy.sensitiveKeywords)
    }

    @Test
    fun resolveWith_nullGroups_inheritAllGlobalGroups() {
        val withGroups = EligibilitySettings(
            textLength = TextLengthSettings(maxLengthChars = 200),
            keywordFilter = KeywordFilterSettings(enabled = true, keywords = listOf("spam")),
            privacy = PrivacySettings(skipOtp = true),
        )
        assertEquals(withGroups, withGroups.resolveWith(AppEligibilityOverride()))
    }

    @Test
    fun resolveWith_textLengthOverride_replacesOnlyThatGroup() {
        val global = EligibilitySettings(
            textLength = TextLengthSettings(maxLengthChars = 200),
            privacy = PrivacySettings(skipOtp = true),
        )
        val resolved = global.resolveWith(
            AppEligibilityOverride(textLength = TextLengthSettings(maxLengthChars = 50)),
        )
        assertEquals(TextLengthSettings(maxLengthChars = 50), resolved.textLength)
        assertEquals(global.privacy, resolved.privacy)
        assertEquals(global.screenCondition, resolved.screenCondition)
    }

    @Test
    fun resolveWith_privacyOverride_replacesOnlyThatGroup() {
        val global = EligibilitySettings(
            keywordFilter = KeywordFilterSettings(enabled = true, keywords = listOf("spam")),
            privacy = PrivacySettings(skipBanking = true),
        )
        val resolved = global.resolveWith(
            AppEligibilityOverride(privacy = PrivacySettings(excludeApp = true)),
        )
        assertEquals(PrivacySettings(excludeApp = true), resolved.privacy)
        assertEquals(global.keywordFilter, resolved.keywordFilter)
        assertEquals(LockCondition.LOCKED, resolved.lockCondition)
    }

    @Test
    fun resolveWith_groupOverride_marksAppAsNotInheritingEveryValue() {
        val override = AppEligibilityOverride(privacy = PrivacySettings(skipOtp = true))
        assertFalse(override.inheritsEveryValue)
    }
}
