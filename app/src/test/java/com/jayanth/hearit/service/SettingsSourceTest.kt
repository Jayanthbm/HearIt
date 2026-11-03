package com.jayanth.hearit.service

import com.jayanth.hearit.core.common.model.AppEligibilityOverride
import com.jayanth.hearit.core.common.model.AppPriority
import com.jayanth.hearit.core.common.model.AppQueueOverride
import com.jayanth.hearit.core.common.model.EligibilitySettings
import com.jayanth.hearit.core.common.model.QueueSettings
import com.jayanth.hearit.core.datastore.AppOverride
import com.jayanth.hearit.core.datastore.HearItSettings
import com.jayanth.hearit.core.datastore.SettingsRepository
import com.jayanth.hearit.core.tts.AnnouncementFormat
import com.jayanth.hearit.core.tts.SpeechSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * L2: the service's settings seam must report exactly what `:core:datastore` persisted —
 * including the master-prompt default that every app starts disabled.
 *
 * These are JVM tests over a fake [SettingsRepository]; DataStore itself is covered by
 * `:core:datastore`'s codec tests.
 */
class SettingsSourceTest {

    private val repository = FakeSettingsRepository()

    private val source = SettingsSource(repository)

    @Test
    fun defaults_everyAppDisabled_andMasterPromptValues() {
        assertFalse(source.forApp("com.whatsapp").appEnabled)
        assertEquals(ResolvedSettingsDefaults.eligibility, source.forApp("com.whatsapp").eligibility)
        assertEquals(AnnouncementFormat(), source.format())
        assertEquals(SpeechSettings(), source.speech())
        assertEquals(QueueSettings(), source.queueSettings())
        assertTrue(source.queueOverrides().isEmpty())
    }

    @Test
    fun reload_enabledApp_isAnnounced_andOtherAppsStayDisabled() = runBlocking {
        repository.update { it.copy(enabledApps = setOf("com.whatsapp")) }

        source.reload()

        assertTrue(source.forApp("com.whatsapp").appEnabled)
        assertFalse(source.forApp("com.signal").appEnabled)
    }

    @Test
    fun reload_perAppOverride_isMergedWithGlobals() = runBlocking {
        repository.update {
            it.copy(
                eligibility = EligibilitySettings(respectDnd = true),
                appOverrides = mapOf(
                    "com.whatsapp" to AppOverride(
                        eligibility = AppEligibilityOverride(respectDnd = false),
                    ),
                ),
            )
        }

        source.reload()

        assertFalse(source.forApp("com.whatsapp").eligibility.respectDnd)
        assertTrue(source.forApp("com.signal").eligibility.respectDnd)
    }

    @Test
    fun reload_priorityList_andPerAppPriorityAreReported() = runBlocking {
        repository.update {
            it.copy(
                priorityAnnounceApps = setOf("com.whatsapp"),
                appOverrides = mapOf(
                    "com.signal" to AppOverride(priority = AppPriority.CRITICAL),
                ),
            )
        }

        source.reload()

        assertTrue(source.isPriorityApp("com.whatsapp"))
        assertFalse(source.isPriorityApp("com.signal"))
        assertTrue(source.forApp("com.whatsapp").priorityAnnounceListed)
        assertEquals(AppPriority.CRITICAL, source.priorityFor("com.signal"))
        assertEquals(AppPriority.NORMAL, source.priorityFor("com.whatsapp"))
    }

    @Test
    fun reload_formatAndSpeechComeFromStoredSettings() = runBlocking {
        repository.update {
            it.copy(
                format = AnnouncementFormat(includeAppName = false, introduction = "New message"),
                speech = SpeechSettings(speechRate = 1.4f, pitch = 0.9f, voiceId = "voice-2"),
            )
        }

        source.reload()

        assertFalse(source.format().includeAppName)
        assertEquals("New message", source.format().introduction)
        assertEquals(1.4f, source.speech().speechRate, 0f)
        assertEquals("voice-2", source.speech().voiceId)
    }

    @Test
    fun queueOverrides_exposeThePerAppGroupingWindow() = runBlocking {
        repository.update {
            it.copy(
                queue = QueueSettings(globalMaxSize = 12),
                appOverrides = mapOf(
                    "com.whatsapp" to AppOverride(
                        queue = AppQueueOverride(groupingWindowMillis = 4_000L),
                    ),
                ),
            )
        }

        source.reload()

        assertEquals(12, source.queueSettings().globalMaxSize)
        assertEquals(4_000L, source.queueOverrides().getValue("com.whatsapp").groupingWindowMillis)
    }

    @Test
    fun observe_mirrorsRepositoryEmissionsWithoutPolling() = runBlocking {
        val job = source.observe(this)

        repository.update { it.copy(enabledApps = setOf("com.whatsapp")) }
        awaitTrue { source.forApp("com.whatsapp").appEnabled }

        repository.update { it.copy(enabledApps = emptySet()) }
        awaitTrue { !source.forApp("com.whatsapp").appEnabled }

        job.cancel()
    }

    @Test
    fun setPriorityApp_persistsThroughTheRepository() = runBlocking {
        source.setPriorityApp("com.whatsapp", listed = true)
        source.reload()

        assertTrue(source.isPriorityApp("com.whatsapp"))
        assertTrue(repository.current().priorityAnnounceApps.contains("com.whatsapp"))

        source.setPriorityApp("com.whatsapp", listed = false)
        source.reload()

        assertFalse(source.isPriorityApp("com.whatsapp"))
    }

    /** Waits for the observed snapshot to catch up with an emission. */
    private suspend fun awaitTrue(predicate: () -> Boolean) {
        withTimeout(2_000L) {
            while (!predicate()) yield()
        }
    }
}

/** Reference defaults, spelled out so a settings-default change is a test failure. */
private object ResolvedSettingsDefaults {
    val eligibility = EligibilitySettings()
}

/** In-memory [SettingsRepository]; stands in for DataStore in JVM tests. */
private class FakeSettingsRepository(initial: HearItSettings = HearItSettings()) :
    SettingsRepository {

    private val state = MutableStateFlow(initial)

    override val settings: Flow<HearItSettings> = state

    override suspend fun current(): HearItSettings = state.value

    override suspend fun update(transform: (HearItSettings) -> HearItSettings) {
        state.value = transform(state.value)
    }

    override suspend fun exportSettings(): String = "{}"

    override suspend fun importSettings(json: String) = Unit
}
