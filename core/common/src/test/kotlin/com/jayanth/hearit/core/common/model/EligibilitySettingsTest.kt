package com.jayanth.hearit.core.common.model

import org.junit.Assert.assertEquals
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
}
