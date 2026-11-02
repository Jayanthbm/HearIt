package com.jayanth.hearit.core.datastore

import com.jayanth.hearit.core.common.model.AppEligibilityOverride
import com.jayanth.hearit.core.common.model.AppHistoryOverride
import com.jayanth.hearit.core.common.model.AppPriority
import com.jayanth.hearit.core.common.model.AppQueueOverride
import com.jayanth.hearit.core.common.model.AudioFocusPolicy
import com.jayanth.hearit.core.common.model.AudioOutputSettings
import com.jayanth.hearit.core.common.model.EligibilitySettings
import com.jayanth.hearit.core.common.model.HistorySettings
import com.jayanth.hearit.core.common.model.HistoryStorageMode
import com.jayanth.hearit.core.common.model.HistoryUpdateHandling
import com.jayanth.hearit.core.common.model.KeywordFilterSettings
import com.jayanth.hearit.core.common.model.KeywordMatchMode
import com.jayanth.hearit.core.common.model.LengthScope
import com.jayanth.hearit.core.common.model.LockCondition
import com.jayanth.hearit.core.common.model.OverflowPolicy
import com.jayanth.hearit.core.common.model.PrivacySettings
import com.jayanth.hearit.core.common.model.QueueSettings
import com.jayanth.hearit.core.common.model.ScreenCondition
import com.jayanth.hearit.core.common.model.TextLengthSettings
import com.jayanth.hearit.core.tts.AnnouncementFormat
import com.jayanth.hearit.core.tts.SpeechSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Schema rules of `SettingsCodec` (written for P09, not executed in the cloud). */
class SettingsCodecTest {

    private fun roundTrip(settings: HearItSettings): HearItSettings =
        SettingsCodec.decode(SettingsCodec.encode(settings))

    private fun customized(): HearItSettings = HearItSettings(
        eligibility = EligibilitySettings(
            screenCondition = ScreenCondition.BOTH,
            lockCondition = LockCondition.UNLOCKED,
            respectDnd = false,
            queueDuringCall = true,
            textLength = TextLengthSettings(maxLengthChars = 500, scope = LengthScope.WHOLE_NOTIFICATION),
            keywordFilter = KeywordFilterSettings(
                enabled = true,
                keywords = listOf("urgent", "hello, world", "line\nbreak"),
                mode = KeywordMatchMode.PHRASE,
            ),
            privacy = PrivacySettings(
                excludeApp = true,
                skipOtp = false,
                skipBanking = true,
                skipPassword = false,
                skipPayment = true,
                hideContent = true,
                sensitiveKeywords = listOf("salary", "passport number"),
            ),
            audioOutput = AudioOutputSettings(
                allowWired = false,
                allowBluetooth = true,
                allowPhoneSpeaker = true,
                allowedBluetoothIds = setOf("AA:BB:CC:DD:EE:FF"),
                allowUnidentifiedOutput = false,
            ),
            audioFocus = AudioFocusPolicy.PAUSE,
        ),
        queue = QueueSettings(
            groupingWindowMillis = 1_000L,
            delayBeforeAnnouncementMillis = 250L,
            maxMessagesPerAnnouncement = 5,
            minGapBetweenAnnouncementsMillis = 750L,
            expiryMillis = 60_000L,
            globalMaxSize = 100,
            perAppMaxSize = 20,
            overflowPolicy = OverflowPolicy.KEEP_HIGHEST_PRIORITY,
            repeatNotificationUpdates = true,
        ),
        format = AnnouncementFormat(
            includeAppName = false,
            includeSender = false,
            includeMessage = true,
            introduction = "New message",
            pauseBetweenSectionsMillis = 300L,
        ),
        speech = SpeechSettings(speechRate = 1.3f, pitch = 0.9f, voiceId = "en-us-x-sfg"),
        history = HistorySettings(
            storageMode = HistoryStorageMode.METADATA_ONLY,
            updateHandling = HistoryUpdateHandling.NEW_ENTRY,
            retentionDays = 30,
            requireAuth = true,
        ),
        enabledApps = setOf("com.whatsapp", "com.example.chat.app"),
        priorityAnnounceApps = setOf("com.whatsapp"),
    )

    @Test
    fun encode_thenDecode_defaults_roundTrip() {
        assertEquals(HearItSettings(), roundTrip(HearItSettings()))
    }

    @Test
    fun decode_emptyMap_returnsShippedDefaults() {
        val decoded = SettingsCodec.decode(emptyMap())

        assertEquals(HearItSettings(), decoded)
        assertTrue(decoded.enabledApps.isEmpty())
        assertTrue(!decoded.eligibility.keywordFilter.enabled)
        assertTrue(!decoded.eligibility.audioOutput.allowPhoneSpeaker)
        assertTrue(decoded.eligibility.privacy.skipOtp)
        assertTrue(!decoded.eligibility.privacy.skipBanking)
        assertEquals(ScreenCondition.OFF, decoded.eligibility.screenCondition)
        assertEquals(LockCondition.LOCKED, decoded.eligibility.lockCondition)
        assertEquals(AudioFocusPolicy.DUCK, decoded.eligibility.audioFocus)
        assertEquals(HistoryStorageMode.FULL_CONTENT, decoded.history.storageMode)
        assertNull(decoded.history.retentionDays)
        assertTrue(!decoded.history.requireAuth)
    }

    @Test
    fun encode_thenDecode_customGlobalSettings_roundTrip() {
        assertEquals(customized(), roundTrip(customized()))
    }

    @Test
    fun encode_writesSchemaVersion() {
        assertEquals(SettingsCodec.SCHEMA_VERSION.toString(), SettingsCodec.encode(HearItSettings())["version"])
    }

    @Test
    fun decode_unknownKeys_areIgnored() {
        val decoded = SettingsCodec.decode(mapOf("version" to "1", "future.key" to "value"))

        assertEquals(HearItSettings(), decoded)
    }

    @Test
    fun decode_invalidEnum_fallsBackToDefault() {
        val decoded = SettingsCodec.decode(mapOf("elig.screen" to "BANANA"))

        assertEquals(ScreenCondition.OFF, decoded.eligibility.screenCondition)
    }

    @Test
    fun decode_invalidQueueNumbers_fallBackToDefaults() {
        val decoded = SettingsCodec.decode(
            mapOf(
                "queue.groupingWindow" to "-5",
                "queue.maxMessages" to "0",
                "queue.expiry" to "0",
                "queue.globalMax" to "abc",
            ),
        )

        assertEquals(QueueSettings(), decoded.queue)
    }

    @Test
    fun decode_invalidSpeechValues_fallBackToDefaults() {
        val decoded = SettingsCodec.decode(mapOf("speech.rate" to "0", "speech.pitch" to "-1"))

        assertEquals(1.0f, decoded.speech.speechRate, 0.0001f)
        assertEquals(1.0f, decoded.speech.pitch, 0.0001f)
    }

    @Test
    fun encode_thenDecode_perAppOverride_roundTrip() {
        val settings = HearItSettings(
            enabledApps = setOf("com.whatsapp"),
            appOverrides = mapOf(
                "com.whatsapp" to AppOverride(
                    eligibility = AppEligibilityOverride(
                        screenCondition = ScreenCondition.ON,
                        audioFocus = AudioFocusPolicy.NONE,
                        textLength = TextLengthSettings(200, LengthScope.MESSAGE),
                        keywordFilter = KeywordFilterSettings(true, listOf("opt"), KeywordMatchMode.WHOLE_WORD),
                        privacy = PrivacySettings(skipBanking = true),
                        audioOutput = AudioOutputSettings(allowPhoneSpeaker = true),
                    ),
                    queue = AppQueueOverride(groupingWindowMillis = 2_000L),
                    history = AppHistoryOverride(
                        storageMode = HistoryStorageMode.DISABLED,
                        requireAuth = true,
                    ),
                    priority = AppPriority.CRITICAL,
                ),
            ),
        )

        assertEquals(settings, roundTrip(settings))
    }

    @Test
    fun decode_absentPerAppFields_areInherited() {
        val settings = HearItSettings(
            appOverrides = mapOf(
                "com.whatsapp" to AppOverride(priority = AppPriority.HIGH),
                "com.telegram" to AppOverride(),
            ),
        )

        val decoded = roundTrip(settings)

        assertEquals(AppPriority.HIGH, decoded.appOverrides.getValue("com.whatsapp").priority)
        assertTrue(!decoded.appOverrides.getValue("com.whatsapp").inheritsEveryValue)
        // An override whose every value is inherited has nothing to store and is dropped,
        // which is exactly the sparseness ADR-021 requires of the settings map.
        assertFalse(decoded.appOverrides.containsKey("com.telegram"))
    }

    @Test
    fun encode_packageNameWithDots_roundTrips() {
        val settings = HearItSettings(
            appOverrides = mapOf(
                "com.example.chat.app" to AppOverride(priority = AppPriority.LOW),
            ),
        )

        assertEquals(settings, roundTrip(settings))
    }

    @Test
    fun encode_listValues_preserveCommasSpacesAndNewlines() {
        val keywords = listOf("hello, world", "line\nbreak", "two words")
        val settings = HearItSettings(
            eligibility = EligibilitySettings(
                keywordFilter = KeywordFilterSettings(enabled = true, keywords = keywords),
            ),
        )

        assertEquals(keywords, roundTrip(settings).eligibility.keywordFilter.keywords)
    }

    @Test
    fun encode_emptyBluetoothAllowList_isDistinctFromAnyBluetooth() {
        val allowNone = HearItSettings(
            eligibility = EligibilitySettings(
                audioOutput = AudioOutputSettings(allowedBluetoothIds = emptySet()),
            ),
        )

        val decoded = roundTrip(allowNone).eligibility.audioOutput

        assertEquals(emptySet<String>(), decoded.allowedBluetoothIds)
        assertNull(roundTrip(HearItSettings()).eligibility.audioOutput.allowedBluetoothIds)
    }

    @Test
    fun decode_withoutVersionKey_stillDecodes() {
        val decoded = SettingsCodec.decode(mapOf("elig.respectDnd" to "false"))

        assertTrue(!decoded.eligibility.respectDnd)
    }

    @Test
    fun encode_thenDecode_everyOverflowPolicy() {
        OverflowPolicy.entries.forEach { policy ->
            val settings = HearItSettings(queue = QueueSettings(overflowPolicy = policy))
            assertEquals(policy, roundTrip(settings).queue.overflowPolicy)
        }
    }

    @Test
    fun encode_nullIntroductionAndRetention_areOmittedAndDecodeNull() {
        val settings = HearItSettings(
            format = AnnouncementFormat(introduction = null),
            history = HistorySettings(retentionDays = null),
        )

        val decoded = roundTrip(settings)

        assertNull(decoded.format.introduction)
        assertNull(decoded.history.retentionDays)
    }

    @Test
    fun encode_thenDecode_textLengthNoLimit_roundTripsAsNull() {
        val settings = HearItSettings(
            eligibility = EligibilitySettings(textLength = TextLengthSettings(maxLengthChars = null)),
        )

        assertNull(roundTrip(settings).eligibility.textLength.maxLengthChars)
    }
}
