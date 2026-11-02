package com.jayanth.hearit.core.datastore

import com.jayanth.hearit.core.common.model.AppEligibilityOverride
import com.jayanth.hearit.core.common.model.AppHistoryOverride
import com.jayanth.hearit.core.common.model.AppPriority
import com.jayanth.hearit.core.common.model.AppQueueOverride
import com.jayanth.hearit.core.common.model.HistoryStorageMode
import com.jayanth.hearit.core.common.model.QueueSettings
import com.jayanth.hearit.core.common.model.ScreenCondition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Typed accessors and inheritance semantics of `HearItSettings` (P09 acceptance). */
class HearItSettingsTest {

    private val whatsApp = "com.whatsapp"

    @Test
    fun defaults_noAppIsEnabled() {
        val settings = HearItSettings()

        assertTrue(settings.enabledApps.isEmpty())
        assertTrue(settings.priorityAnnounceApps.isEmpty())
        assertTrue(settings.appOverrides.isEmpty())
    }

    @Test
    fun resolvedFor_unknownApp_isDisabled() {
        assertFalse(HearItSettings().resolvedFor(whatsApp).appEnabled)
    }

    @Test
    fun resolvedFor_enabledApp_isEnabledWithGlobalValues() {
        val settings = HearItSettings(enabledApps = setOf(whatsApp))

        val resolved = settings.resolvedFor(whatsApp)

        assertTrue(resolved.appEnabled)
        assertFalse(resolved.priorityAnnounceListed)
        assertEquals(settings.eligibility, resolved.eligibility)
    }

    @Test
    fun resolvedFor_priorityListed_isListedIndependentlyOfEnabled() {
        val settings = HearItSettings(priorityAnnounceApps = setOf(whatsApp))

        val resolved = settings.resolvedFor(whatsApp)

        assertFalse(resolved.appEnabled)
        assertTrue(resolved.priorityAnnounceListed)
    }

    @Test
    fun resolvedFor_appOverride_replacesOnlyThatCondition() {
        val settings = HearItSettings(
            enabledApps = setOf(whatsApp),
            appOverrides = mapOf(
                whatsApp to AppOverride(
                    eligibility = AppEligibilityOverride(screenCondition = ScreenCondition.BOTH),
                ),
            ),
        )

        val resolved = settings.resolvedFor(whatsApp)

        assertEquals(ScreenCondition.BOTH, resolved.eligibility.screenCondition)
        // Everything else still inherits the global value.
        assertEquals(settings.eligibility.lockCondition, resolved.eligibility.lockCondition)
        assertEquals(settings.eligibility.privacy, resolved.eligibility.privacy)
    }

    @Test
    fun queueFor_withoutOverride_inheritsGlobal() {
        val settings = HearItSettings(queue = QueueSettings(groupingWindowMillis = 9_000L))

        assertEquals(9_000L, settings.queueFor(whatsApp).groupingWindowMillis)
    }

    @Test
    fun queueFor_override_appliesGroupingWindowOnly() {
        val settings = HearItSettings(
            queue = QueueSettings(groupingWindowMillis = 9_000L, maxMessagesPerAnnouncement = 4),
            appOverrides = mapOf(
                whatsApp to AppOverride(queue = AppQueueOverride(groupingWindowMillis = 1_000L)),
            ),
        )

        val resolved = settings.queueFor(whatsApp)

        assertEquals(1_000L, resolved.groupingWindowMillis)
        assertEquals(4, resolved.maxMessagesPerAnnouncement)
    }

    @Test
    fun historyFor_override_appliesStorageMode() {
        val settings = HearItSettings(
            appOverrides = mapOf(
                whatsApp to AppOverride(
                    history = AppHistoryOverride(storageMode = HistoryStorageMode.DISABLED),
                ),
            ),
        )

        assertEquals(HistoryStorageMode.DISABLED, settings.historyFor(whatsApp).storageMode)
        assertEquals(HistoryStorageMode.FULL_CONTENT, settings.historyFor("com.telegram").storageMode)
    }

    @Test
    fun priorityFor_defaultsToNormal() {
        assertEquals(AppPriority.NORMAL, HearItSettings().priorityFor(whatsApp))
    }

    @Test
    fun priorityFor_override_returnsOverride() {
        val settings = HearItSettings(
            appOverrides = mapOf(whatsApp to AppOverride(priority = AppPriority.CRITICAL)),
        )

        assertEquals(AppPriority.CRITICAL, settings.priorityFor(whatsApp))
    }

    @Test
    fun appOverride_inheritsEveryValue_trueForEmptyAndFalseWhenAnyFieldSet() {
        assertTrue(AppOverride().inheritsEveryValue)
        assertFalse(AppOverride(priority = AppPriority.LOW).inheritsEveryValue)
        assertFalse(
            AppOverride(queue = AppQueueOverride(groupingWindowMillis = 0L)).inheritsEveryValue,
        )
    }
}
