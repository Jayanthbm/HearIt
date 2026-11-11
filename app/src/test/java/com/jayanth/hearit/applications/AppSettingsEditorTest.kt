package com.jayanth.hearit.applications

import com.jayanth.hearit.core.common.model.AppPriority
import com.jayanth.hearit.core.common.model.AudioFocusPolicy
import com.jayanth.hearit.core.common.model.HistoryStorageMode
import com.jayanth.hearit.core.common.model.HistoryUpdateHandling
import com.jayanth.hearit.core.common.model.KeywordMatchMode
import com.jayanth.hearit.core.datastore.AppOverride
import com.jayanth.hearit.core.datastore.HearItSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Per-app editing and inheritance tests (P13). Written, **not executed** — cloud-agent
 * restriction.
 *
 * They pin the ADR-021 contract the UI relies on: `null` means inherit, and an
 * all-inherited app leaves no entry behind.
 */
class AppSettingsEditorTest {

    private val packageName = "com.whatsapp"

    @Test
    fun toggleEnabled_addsThenRemoves() {
        val once = AppSettingsEditor.toggleEnabled(HearItSettings(), packageName)
        assertTrue(packageName in once.enabledApps)

        val twice = AppSettingsEditor.toggleEnabled(once, packageName)
        assertFalse(packageName in twice.enabledApps)
    }

    @Test
    fun setEnabled_leavesOtherAppsAlone() {
        val settings = HearItSettings(enabledApps = setOf("com.other"))

        val disabled = AppSettingsEditor.setEnabled(settings, packageName, enabled = false)

        assertEquals(setOf("com.other"), disabled.enabledApps)
    }

    @Test
    fun enableAll_addsVisibleAppsAndKeepsOthers() {
        val settings = HearItSettings(enabledApps = setOf("com.other"))

        val updated = AppSettingsEditor.enableAll(settings, listOf(packageName, "com.example.bank"))

        assertEquals(setOf("com.other", packageName, "com.example.bank"), updated.enabledApps)
    }

    @Test
    fun disableAll_removesVisibleAppsAndKeepsOthers() {
        val settings = HearItSettings(enabledApps = setOf("com.other", packageName))

        val updated = AppSettingsEditor.disableAll(settings, listOf(packageName))

        assertEquals(setOf("com.other"), updated.enabledApps)
    }

    @Test
    fun setPriority_storesSparseOverride() {
        val updated = AppSettingsEditor.setPriority(HearItSettings(), packageName, AppPriority.HIGH)

        assertEquals(AppPriority.HIGH, updated.appOverrides[packageName]?.priority)
        assertEquals(AppPriority.HIGH, updated.priorityFor(packageName))
    }

    @Test
    fun setPriorityNull_removesTheEntryAgain() {
        val withOverride = AppSettingsEditor.setPriority(HearItSettings(), packageName, AppPriority.CRITICAL)

        val cleared = AppSettingsEditor.setPriority(withOverride, packageName, null)

        assertNull(cleared.appOverrides[packageName])
        assertEquals(AppPriority.NORMAL, cleared.priorityFor(packageName))
    }

    @Test
    fun setHistoryStorageMode_storesAndInheritsViaNull() {
        val overridden = AppSettingsEditor.setHistoryStorageMode(
            HearItSettings(),
            packageName,
            HistoryStorageMode.METADATA_ONLY,
        )
        assertEquals(HistoryStorageMode.METADATA_ONLY, overridden.historyFor(packageName).storageMode)

        val inherited = AppSettingsEditor.setHistoryStorageMode(overridden, packageName, null)

        assertNull(inherited.appOverrides[packageName])
        assertEquals(HistoryStorageMode.FULL_CONTENT, inherited.historyFor(packageName).storageMode)
    }

    @Test
    fun setHistoryUpdateHandling_storesOverride() {
        val updated = AppSettingsEditor.setHistoryUpdateHandling(
            HearItSettings(),
            packageName,
            HistoryUpdateHandling.NEW_ENTRY,
        )

        assertEquals(HistoryUpdateHandling.NEW_ENTRY, updated.historyFor(packageName).updateHandling)
    }

    @Test
    fun setRespectDnd_storesOverride() {
        val updated = AppSettingsEditor.setRespectDnd(HearItSettings(), packageName, false)

        assertFalse(updated.resolvedFor(packageName).eligibility.respectDnd)
    }

    @Test
    fun setQueueDuringCall_storesOverride() {
        val updated = AppSettingsEditor.setQueueDuringCall(HearItSettings(), packageName, true)

        assertTrue(updated.resolvedFor(packageName).eligibility.queueDuringCall)
    }

    @Test
    fun setAudioFocus_storesOverride() {
        val updated = AppSettingsEditor.setAudioFocus(HearItSettings(), packageName, AudioFocusPolicy.PAUSE)

        assertEquals(AudioFocusPolicy.PAUSE, updated.resolvedFor(packageName).eligibility.audioFocus)
    }

    @Test
    fun oneOverrideDoesNotClearAnother() {
        val withPriority = AppSettingsEditor.setPriority(HearItSettings(), packageName, AppPriority.LOW)

        val withBoth = AppSettingsEditor.setRespectDnd(withPriority, packageName, false)

        assertEquals(AppPriority.LOW, withBoth.appOverrides[packageName]?.priority)
        assertFalse(withBoth.resolvedFor(packageName).eligibility.respectDnd)
    }

    @Test
    fun resetToGlobal_clearsOverridesButKeepsTheAnnouncementSwitch() {
        val settings = HearItSettings(enabledApps = setOf(packageName))
            .let { AppSettingsEditor.setPriority(it, packageName, AppPriority.HIGH) }

        val reset = AppSettingsEditor.resetToGlobal(settings, packageName)

        assertNull(reset.appOverrides[packageName])
        assertTrue(packageName in reset.enabledApps)
    }

    @Test
    fun settingsFor_reportsInheritanceAndResolvedValues() {
        val state = AppSettingsEditor.settingsFor(HearItSettings(), packageName, "WhatsApp")

        assertEquals("WhatsApp", state.label)
        assertTrue(state.priority.isInherited)
        assertEquals(AppPriority.NORMAL, state.priority.effective)
        assertTrue(state.historyStorageMode.isInherited)
        assertEquals(HistoryStorageMode.FULL_CONTENT, state.historyStorageMode.effective)
        assertTrue(state.respectDnd.isInherited)
        assertTrue(state.respectDnd.effective)
        assertFalse(state.queueDuringCall.effective)
        assertEquals(AudioFocusPolicy.DUCK, state.audioFocus.effective)
        assertFalse(state.hasOverrides)
    }

    @Test
    fun settingsFor_reflectsOverrides() {
        val settings = HearItSettings(enabledApps = setOf(packageName))
            .let { AppSettingsEditor.setPriority(it, packageName, AppPriority.CRITICAL) }
            .let { AppSettingsEditor.setRespectDnd(it, packageName, false) }

        val state = AppSettingsEditor.settingsFor(settings, packageName, "WhatsApp")

        assertFalse(state.priority.isInherited)
        assertEquals(AppPriority.CRITICAL, state.priority.effective)
        assertFalse(state.respectDnd.effective)
        assertTrue(state.enabled)
        assertTrue(state.hasOverrides)
        assertEquals(1, state.enabledAppCount)
    }

    @Test
    fun settingsFor_blankLabel_fallsBackToPackageName() {
        val state = AppSettingsEditor.settingsFor(HearItSettings(), packageName, "   ")

        assertEquals(packageName, state.label)
    }

    @Test
    fun settingsFor_unrelatedAppKeepsItsOwnInheritance() {
        val settings = HearItSettings(
            appOverrides = mapOf("com.other" to AppOverride(priority = AppPriority.HIGH)),
        )

        val state = AppSettingsEditor.settingsFor(settings, packageName, "WhatsApp")

        assertTrue(state.priority.isInherited)
        assertFalse(state.hasOverrides)
    }

    @Test
    fun addKeyword_appendsAndEnablesFilter() {
        val updated = AppSettingsEditor.addKeyword(HearItSettings(), packageName, "OTP", KeywordMatchMode.EXACT)
        val state = AppSettingsEditor.settingsFor(updated, packageName, "BankApp")

        assertFalse(state.keywordFilter.isInherited)
        assertTrue(state.keywordFilter.effective.enabled)
        assertEquals(listOf("exact:OTP"), state.keywordFilter.effective.keywords)
        assertEquals(KeywordMatchMode.EXACT, state.keywordFilter.effective.mode)
    }

    @Test
    fun removeKeyword_removesAndInheritsWhenEmpty() {
        val withKeyword = AppSettingsEditor.addKeyword(HearItSettings(), packageName, "OTP")
        val removed = AppSettingsEditor.removeKeyword(withKeyword, packageName, "OTP")
        val state = AppSettingsEditor.settingsFor(removed, packageName, "BankApp")

        assertTrue(state.keywordFilter.isInherited)
    }

    @Test
    fun setKeywordMatchMode_updatesMode() {
        val withKeyword = AppSettingsEditor.addKeyword(HearItSettings(), packageName, "order")
        val withMode = AppSettingsEditor.setKeywordMatchMode(withKeyword, packageName, KeywordMatchMode.PHRASE)
        val state = AppSettingsEditor.settingsFor(withMode, packageName, "Shopping")

        assertEquals(KeywordMatchMode.PHRASE, state.keywordFilter.effective.mode)
    }

    @Test
    fun speechAndAutoDismissOverrides_applyAndRevertToGlobal() {
        var updated = AppSettingsEditor.setSpeechRate(HearItSettings(), packageName, 1.35f)
        updated = AppSettingsEditor.setSpeechPitch(updated, packageName, 0.9f)
        updated = AppSettingsEditor.setSpeechVoiceId(updated, packageName, "voice-a")
        updated = AppSettingsEditor.setAutoDismiss(updated, packageName, false)

        val state = AppSettingsEditor.settingsFor(updated, packageName, "WhatsApp", globalAutoDismiss = true)
        assertEquals(1.35f, state.speechRate.effective, 0.0001f)
        assertEquals(0.9f, state.speechPitch.effective, 0.0001f)
        assertEquals("voice-a", state.speechVoiceId.effective)
        assertFalse(state.autoDismiss.effective)

        val reset = AppSettingsEditor.resetToGlobal(updated, packageName)
        val resetState = AppSettingsEditor.settingsFor(reset, packageName, "WhatsApp", globalAutoDismiss = true)
        assertTrue(resetState.speechRate.isInherited)
        assertTrue(resetState.autoDismiss.isInherited)
        assertTrue(resetState.autoDismiss.effective)
    }

    @Test
    fun priorityAnnounceAndBatchActions_workAcrossMultiplePackages() {
        val pkgs = listOf("com.a", "com.b")
        var settings = AppSettingsEditor.enableAll(HearItSettings(), pkgs)
        settings = AppSettingsEditor.batchSetPriorityAnnounce(settings, pkgs, true)
        assertTrue("com.a" in settings.priorityAnnounceApps)
        assertTrue("com.b" in settings.priorityAnnounceApps)

        settings = AppSettingsEditor.batchSetPriority(settings, pkgs, com.jayanth.hearit.core.common.model.AppPriority.HIGH)
        assertEquals(com.jayanth.hearit.core.common.model.AppPriority.HIGH, settings.priorityFor("com.a"))

        settings = AppSettingsEditor.batchResetToGlobal(settings, pkgs)
        assertFalse(settings.appOverrides.containsKey("com.a"))
        assertFalse(settings.appOverrides.containsKey("com.b"))
    }
}
