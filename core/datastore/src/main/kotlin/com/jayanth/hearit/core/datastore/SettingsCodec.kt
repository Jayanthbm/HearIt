package com.jayanth.hearit.core.datastore

import com.jayanth.hearit.core.common.model.AppCategoryChoice
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
import com.jayanth.hearit.core.common.model.KnownAudioDevice
import com.jayanth.hearit.core.common.model.LengthScope
import com.jayanth.hearit.core.common.model.LockCondition
import com.jayanth.hearit.core.common.model.OverflowPolicy
import com.jayanth.hearit.core.common.model.PrivacySettings
import com.jayanth.hearit.core.common.model.QueueSettings
import com.jayanth.hearit.core.common.model.ScreenCondition
import com.jayanth.hearit.core.common.model.SecuritySettings
import com.jayanth.hearit.core.common.model.TextLengthSettings
import com.jayanth.hearit.core.tts.AnnouncementFormat
import com.jayanth.hearit.core.tts.SpeechSettings

/**
 * Maps [HearItSettings] onto a flat, stable string map and back (P09).
 *
 * Pure Kotlin: persistence (DataStore) is just a `Map<String, String>` behind
 * [PreferenceStore], so the whole schema is JVM-unit-tested without Android.
 *
 * Schema rules:
 *  - **Global values** use fixed keys (`elig.*`, `queue.*`, `format.*`, `speech.*`,
 *    `history.*`, `apps.*`) and always encode every non-optional value.
 *  - **Per-app overrides** use `app:<packageName>:<field>`, sparse by design: a key that is
 *    **absent means "inherit the global value"** — the storage-level expression of
 *    "Inherits global" (master prompt §5, P09 acceptance). Package names cannot contain `:`,
 *    so splitting with `limit = 3` is unambiguous even with dots in the package.
 *  - **Whole-group overrides** (text length, keywords, privacy, output) are present when
 *    their first key is present and are then decoded as a complete group with the model's
 *    defaults for any missing field (encoder always writes the whole group).
 *  - **Lists** are joined by the ASCII unit separator; that character is stripped from
 *    values on encode so any user text (commas, newlines, spaces) round-trips.
 *  - **Tolerant decode**: unknown keys are ignored, unknown enum names and invalid numbers
 *    fall back to the documented default instead of throwing, so a corrupted or newer
 *    store can never crash the app. `version` records the schema for future migrations.
 */
object SettingsCodec {

    /** Bump together with a migration when keys change meaning (none yet). */
    const val SCHEMA_VERSION: Int = 1

    private const val LIST_SEPARATOR = "\u001F"

    /** Separates the fields of one remembered device; never used inside a list value. */
    private const val FIELD_SEPARATOR = "\u001E"
    private const val APP_PREFIX = "app:"

    private const val KEY_VERSION = "version"

    private const val KEY_SCREEN = "elig.screen"
    private const val KEY_LOCK = "elig.lock"
    private const val KEY_RESPECT_DND = "elig.respectDnd"
    private const val KEY_QUEUE_DURING_CALL = "elig.queueDuringCall"
    private const val KEY_AUDIO_FOCUS = "elig.audioFocus"
    private const val KEY_LENGTH_MAX = "elig.length.max"
    private const val KEY_LENGTH_SCOPE = "elig.length.scope"
    private const val KEY_KEYWORDS_ENABLED = "elig.keywords.enabled"
    private const val KEY_KEYWORDS_MODE = "elig.keywords.mode"
    private const val KEY_KEYWORDS_LIST = "elig.keywords.list"
    private const val KEY_PRIVACY_EXCLUDE = "elig.privacy.excludeApp"
    private const val KEY_PRIVACY_OTP = "elig.privacy.skipOtp"
    private const val KEY_PRIVACY_BANKING = "elig.privacy.skipBanking"
    private const val KEY_PRIVACY_PASSWORD = "elig.privacy.skipPassword"
    private const val KEY_PRIVACY_PAYMENT = "elig.privacy.skipPayment"
    private const val KEY_PRIVACY_HIDE = "elig.privacy.hideContent"
    private const val KEY_PRIVACY_KEYWORDS = "elig.privacy.sensitiveKeywords"
    private const val KEY_AUDIO_WIRED = "elig.audio.allowWired"
    private const val KEY_AUDIO_BLUETOOTH = "elig.audio.allowBluetooth"
    private const val KEY_AUDIO_SPEAKER = "elig.audio.allowPhoneSpeaker"
    private const val KEY_AUDIO_BT_IDS = "elig.audio.allowedBluetoothIds"
    private const val KEY_AUDIO_UNIDENTIFIED = "elig.audio.allowUnidentifiedOutput"

    private const val KEY_QUEUE_WINDOW = "queue.groupingWindow"
    private const val KEY_QUEUE_DELAY = "queue.delay"
    private const val KEY_QUEUE_MAX_MESSAGES = "queue.maxMessages"
    private const val KEY_QUEUE_MIN_GAP = "queue.minGap"
    private const val KEY_QUEUE_EXPIRY = "queue.expiry"
    private const val KEY_QUEUE_GLOBAL_MAX = "queue.globalMax"
    private const val KEY_QUEUE_PER_APP_MAX = "queue.perAppMax"
    private const val KEY_QUEUE_OVERFLOW = "queue.overflow"
    private const val KEY_QUEUE_REPEAT = "queue.repeatUpdates"

    private const val KEY_FORMAT_APP = "format.includeAppName"
    private const val KEY_FORMAT_SENDER = "format.includeSender"
    private const val KEY_FORMAT_MESSAGE = "format.includeMessage"
    private const val KEY_FORMAT_INTRO = "format.introduction"
    private const val KEY_FORMAT_PAUSE = "format.pause"

    private const val KEY_SPEECH_RATE = "speech.rate"
    private const val KEY_SPEECH_PITCH = "speech.pitch"
    private const val KEY_SPEECH_VOICE = "speech.voice"
    private const val KEY_SPEECH_VOLUME = "speech.volume"

    private const val KEY_HISTORY_MODE = "history.storageMode"
    private const val KEY_HISTORY_UPDATES = "history.updateHandling"
    private const val KEY_HISTORY_RETENTION = "history.retentionDays"
    private const val KEY_HISTORY_AUTH = "history.requireAuth"

    private const val KEY_SECURITY_BIOMETRIC = "security.biometricLock"
    private const val KEY_SECURITY_FLAG_SECURE = "security.secureScreen"

    private const val KEY_ENABLED_APPS = "apps.enabled"
    private const val KEY_PRIORITY_APPS = "apps.priorityAnnounce"
    private const val KEY_KNOWN_DEVICES = "devices.known"

    // Per-app suffixes (used after "app:<package>:").
    private const val APP_PRIORITY = "priority"
    private const val APP_SPOKEN_NAME = "spokenName"
    private const val APP_CATEGORY = "category"
    private const val APP_QUEUE_WINDOW = "queue.groupingWindow"
    private const val APP_HISTORY_MODE = "history.storageMode"
    private const val APP_HISTORY_UPDATES = "history.updateHandling"
    private const val APP_HISTORY_RETENTION = "history.retention"
    private const val APP_HISTORY_AUTH = "history.requireAuth"
    private const val APP_LENGTH_SCOPE = "elig.length.scope"
    private const val APP_LENGTH_MAX = "elig.length.max"
    private const val APP_KEYWORDS_ENABLED = "elig.keywords.enabled"
    private const val APP_KEYWORDS_MODE = "elig.keywords.mode"
    private const val APP_KEYWORDS_LIST = "elig.keywords.list"
    private const val APP_PRIVACY_EXCLUDE = "elig.privacy.excludeApp"
    private const val APP_PRIVACY_OTP = "elig.privacy.skipOtp"
    private const val APP_PRIVACY_BANKING = "elig.privacy.skipBanking"
    private const val APP_PRIVACY_PASSWORD = "elig.privacy.skipPassword"
    private const val APP_PRIVACY_PAYMENT = "elig.privacy.skipPayment"
    private const val APP_PRIVACY_HIDE = "elig.privacy.hideContent"
    private const val APP_PRIVACY_KEYWORDS = "elig.privacy.sensitiveKeywords"
    private const val APP_AUDIO_WIRED = "elig.audio.allowWired"
    private const val APP_AUDIO_BLUETOOTH = "elig.audio.allowBluetooth"
    private const val APP_AUDIO_SPEAKER = "elig.audio.allowPhoneSpeaker"
    private const val APP_AUDIO_BT_IDS = "elig.audio.allowedBluetoothIds"
    private const val APP_AUDIO_UNIDENTIFIED = "elig.audio.allowUnidentifiedOutput"

    /** Encodes [settings] into the persisted map (never null; absent = inherit/default). */
    fun encode(settings: HearItSettings): Map<String, String> {
        val values = LinkedHashMap<String, String>()
        values[KEY_VERSION] = SCHEMA_VERSION.toString()

        val eligibility = settings.eligibility
        values[KEY_SCREEN] = eligibility.screenCondition.name
        values[KEY_LOCK] = eligibility.lockCondition.name
        values[KEY_RESPECT_DND] = eligibility.respectDnd.toString()
        values[KEY_QUEUE_DURING_CALL] = eligibility.queueDuringCall.toString()
        values[KEY_AUDIO_FOCUS] = eligibility.audioFocus.name
        eligibility.textLength.maxLengthChars?.let { values[KEY_LENGTH_MAX] = it.toString() }
        values[KEY_LENGTH_SCOPE] = eligibility.textLength.scope.name
        values[KEY_KEYWORDS_ENABLED] = eligibility.keywordFilter.enabled.toString()
        values[KEY_KEYWORDS_MODE] = eligibility.keywordFilter.mode.name
        values[KEY_KEYWORDS_LIST] = encodeList(eligibility.keywordFilter.keywords)

        val privacy = eligibility.privacy
        values[KEY_PRIVACY_EXCLUDE] = privacy.excludeApp.toString()
        values[KEY_PRIVACY_OTP] = privacy.skipOtp.toString()
        values[KEY_PRIVACY_BANKING] = privacy.skipBanking.toString()
        values[KEY_PRIVACY_PASSWORD] = privacy.skipPassword.toString()
        values[KEY_PRIVACY_PAYMENT] = privacy.skipPayment.toString()
        values[KEY_PRIVACY_HIDE] = privacy.hideContent.toString()
        values[KEY_PRIVACY_KEYWORDS] = encodeList(privacy.sensitiveKeywords)

        val audio = eligibility.audioOutput
        values[KEY_AUDIO_WIRED] = audio.allowWired.toString()
        values[KEY_AUDIO_BLUETOOTH] = audio.allowBluetooth.toString()
        values[KEY_AUDIO_SPEAKER] = audio.allowPhoneSpeaker.toString()
        audio.allowedBluetoothIds?.let { values[KEY_AUDIO_BT_IDS] = encodeList(it) }
        values[KEY_AUDIO_UNIDENTIFIED] = audio.allowUnidentifiedOutput.toString()

        val queue = settings.queue
        values[KEY_QUEUE_WINDOW] = queue.groupingWindowMillis.toString()
        values[KEY_QUEUE_DELAY] = queue.delayBeforeAnnouncementMillis.toString()
        values[KEY_QUEUE_MAX_MESSAGES] = queue.maxMessagesPerAnnouncement.toString()
        values[KEY_QUEUE_MIN_GAP] = queue.minGapBetweenAnnouncementsMillis.toString()
        values[KEY_QUEUE_EXPIRY] = queue.expiryMillis.toString()
        values[KEY_QUEUE_GLOBAL_MAX] = queue.globalMaxSize.toString()
        values[KEY_QUEUE_PER_APP_MAX] = queue.perAppMaxSize.toString()
        values[KEY_QUEUE_OVERFLOW] = queue.overflowPolicy.name
        values[KEY_QUEUE_REPEAT] = queue.repeatNotificationUpdates.toString()

        val format = settings.format
        values[KEY_FORMAT_APP] = format.includeAppName.toString()
        values[KEY_FORMAT_SENDER] = format.includeSender.toString()
        values[KEY_FORMAT_MESSAGE] = format.includeMessage.toString()
        format.introduction?.let { values[KEY_FORMAT_INTRO] = it }
        values[KEY_FORMAT_PAUSE] = format.pauseBetweenSectionsMillis.toString()

        val speech = settings.speech
        values[KEY_SPEECH_RATE] = speech.speechRate.toString()
        values[KEY_SPEECH_PITCH] = speech.pitch.toString()
        speech.voiceId?.let { values[KEY_SPEECH_VOICE] = it }
        values[KEY_SPEECH_VOLUME] = speech.volumeLevel.toString()

        val history = settings.history
        values[KEY_HISTORY_MODE] = history.storageMode.name
        values[KEY_HISTORY_UPDATES] = history.updateHandling.name
        history.retentionDays?.let { values[KEY_HISTORY_RETENTION] = it.toString() }
        values[KEY_HISTORY_AUTH] = history.requireAuth.toString()

        val security = settings.security
        values[KEY_SECURITY_BIOMETRIC] = security.biometricLockEnabled.toString()
        values[KEY_SECURITY_FLAG_SECURE] = security.secureScreenEnabled.toString()

        values[KEY_ENABLED_APPS] = encodeList(settings.enabledApps)
        values[KEY_PRIORITY_APPS] = encodeList(settings.priorityAnnounceApps)
        values[KEY_KNOWN_DEVICES] = encodeDevices(settings.knownDevices)

        settings.appOverrides.forEach { (packageName, override) ->
            encodeOverride(packageName, override, values)
        }
        return values
    }

    /** Decodes a persisted map; missing keys use the documented defaults. */
    fun decode(values: Map<String, String>): HearItSettings = HearItSettings(
        eligibility = decodeEligibility(values),
        queue = decodeQueue(values),
        format = decodeFormat(values),
        speech = decodeSpeech(values),
        history = decodeHistory(values),
        security = decodeSecurity(values),
        enabledApps = splitList(values[KEY_ENABLED_APPS]).filter { it.isNotBlank() }.toSet(),
        priorityAnnounceApps = splitList(values[KEY_PRIORITY_APPS]).filter { it.isNotBlank() }.toSet(),
        knownDevices = decodeDevices(values[KEY_KNOWN_DEVICES]),
        appOverrides = decodeOverrides(values),
    )

    private fun encodeOverride(
        packageName: String,
        override: AppOverride,
        values: MutableMap<String, String>,
    ) {
        val prefix = "$APP_PREFIX$packageName:"

        val eligibility = override.eligibility
        eligibility.screenCondition?.let { values[prefix + "elig.screen"] = it.name }
        eligibility.lockCondition?.let { values[prefix + "elig.lock"] = it.name }
        eligibility.respectDnd?.let { values[prefix + "elig.respectDnd"] = it.toString() }
        eligibility.queueDuringCall?.let { values[prefix + "elig.queueDuringCall"] = it.toString() }
        eligibility.audioFocus?.let { values[prefix + "elig.audioFocus"] = it.name }

        eligibility.textLength?.let { group ->
            values[prefix + APP_LENGTH_SCOPE] = group.scope.name
            group.maxLengthChars?.let { values[prefix + APP_LENGTH_MAX] = it.toString() }
        }
        eligibility.keywordFilter?.let { group ->
            values[prefix + APP_KEYWORDS_ENABLED] = group.enabled.toString()
            values[prefix + APP_KEYWORDS_MODE] = group.mode.name
            values[prefix + APP_KEYWORDS_LIST] = encodeList(group.keywords)
        }
        eligibility.privacy?.let { group ->
            values[prefix + APP_PRIVACY_EXCLUDE] = group.excludeApp.toString()
            values[prefix + APP_PRIVACY_OTP] = group.skipOtp.toString()
            values[prefix + APP_PRIVACY_BANKING] = group.skipBanking.toString()
            values[prefix + APP_PRIVACY_PASSWORD] = group.skipPassword.toString()
            values[prefix + APP_PRIVACY_PAYMENT] = group.skipPayment.toString()
            values[prefix + APP_PRIVACY_HIDE] = group.hideContent.toString()
            values[prefix + APP_PRIVACY_KEYWORDS] = encodeList(group.sensitiveKeywords)
        }
        eligibility.audioOutput?.let { group ->
            values[prefix + APP_AUDIO_WIRED] = group.allowWired.toString()
            values[prefix + APP_AUDIO_BLUETOOTH] = group.allowBluetooth.toString()
            values[prefix + APP_AUDIO_SPEAKER] = group.allowPhoneSpeaker.toString()
            group.allowedBluetoothIds?.let { values[prefix + APP_AUDIO_BT_IDS] = encodeList(it) }
            values[prefix + APP_AUDIO_UNIDENTIFIED] = group.allowUnidentifiedOutput.toString()
        }

        override.queue.groupingWindowMillis?.let { values[prefix + APP_QUEUE_WINDOW] = it.toString() }
        override.history.storageMode?.let { values[prefix + APP_HISTORY_MODE] = it.name }
        override.history.updateHandling?.let { values[prefix + APP_HISTORY_UPDATES] = it.name }
        override.history.retentionDays?.let { values[prefix + APP_HISTORY_RETENTION] = it.toString() }
        override.history.requireAuth?.let { values[prefix + APP_HISTORY_AUTH] = it.toString() }
        override.priority?.let { values[prefix + APP_PRIORITY] = it.name }
        override.spokenName?.let { values[prefix + APP_SPOKEN_NAME] = it }
        override.categoryOverride?.let { values[prefix + APP_CATEGORY] = it.name }
    }

    private fun decodeEligibility(values: Map<String, String>): EligibilitySettings {
        val defaults = EligibilitySettings()
        return EligibilitySettings(
            screenCondition = values.enum(KEY_SCREEN, defaults.screenCondition),
            lockCondition = values.enum(KEY_LOCK, defaults.lockCondition),
            respectDnd = values.boolean(KEY_RESPECT_DND, defaults.respectDnd),
            queueDuringCall = values.boolean(KEY_QUEUE_DURING_CALL, defaults.queueDuringCall),
            textLength = TextLengthSettings(
                maxLengthChars = values[KEY_LENGTH_MAX]?.toIntOrNull()?.takeIf { it > 0 },
                scope = values.enum(KEY_LENGTH_SCOPE, defaults.textLength.scope),
            ),
            keywordFilter = KeywordFilterSettings(
                enabled = values.boolean(KEY_KEYWORDS_ENABLED, defaults.keywordFilter.enabled),
                keywords = splitList(values[KEY_KEYWORDS_LIST]),
                mode = values.enum(KEY_KEYWORDS_MODE, defaults.keywordFilter.mode),
            ),
            privacy = PrivacySettings(
                excludeApp = values.boolean(KEY_PRIVACY_EXCLUDE, defaults.privacy.excludeApp),
                skipOtp = values.boolean(KEY_PRIVACY_OTP, defaults.privacy.skipOtp),
                skipBanking = values.boolean(KEY_PRIVACY_BANKING, defaults.privacy.skipBanking),
                skipPassword = values.boolean(KEY_PRIVACY_PASSWORD, defaults.privacy.skipPassword),
                skipPayment = values.boolean(KEY_PRIVACY_PAYMENT, defaults.privacy.skipPayment),
                hideContent = values.boolean(KEY_PRIVACY_HIDE, defaults.privacy.hideContent),
                sensitiveKeywords = splitList(values[KEY_PRIVACY_KEYWORDS]),
            ),
            audioOutput = AudioOutputSettings(
                allowWired = values.boolean(KEY_AUDIO_WIRED, defaults.audioOutput.allowWired),
                allowBluetooth = values.boolean(KEY_AUDIO_BLUETOOTH, defaults.audioOutput.allowBluetooth),
                allowPhoneSpeaker = values.boolean(KEY_AUDIO_SPEAKER, defaults.audioOutput.allowPhoneSpeaker),
                allowedBluetoothIds = values[KEY_AUDIO_BT_IDS]?.let { raw ->
                    splitList(raw).filter { it.isNotBlank() }.toSet()
                },
                allowUnidentifiedOutput = values.boolean(
                    KEY_AUDIO_UNIDENTIFIED,
                    defaults.audioOutput.allowUnidentifiedOutput,
                ),
            ),
            audioFocus = values.enum(KEY_AUDIO_FOCUS, defaults.audioFocus),
        )
    }

    private fun decodeQueue(values: Map<String, String>): QueueSettings {
        val defaults = QueueSettings()
        return QueueSettings(
            groupingWindowMillis = values.long(KEY_QUEUE_WINDOW, defaults.groupingWindowMillis, minimum = 0L),
            delayBeforeAnnouncementMillis = values.long(
                KEY_QUEUE_DELAY,
                defaults.delayBeforeAnnouncementMillis,
                minimum = 0L,
            ),
            maxMessagesPerAnnouncement = values.int(
                KEY_QUEUE_MAX_MESSAGES,
                defaults.maxMessagesPerAnnouncement,
                minimum = 1,
            ),
            minGapBetweenAnnouncementsMillis = values.long(
                KEY_QUEUE_MIN_GAP,
                defaults.minGapBetweenAnnouncementsMillis,
                minimum = 0L,
            ),
            expiryMillis = values.long(KEY_QUEUE_EXPIRY, defaults.expiryMillis, minimum = 1L),
            globalMaxSize = values.int(KEY_QUEUE_GLOBAL_MAX, defaults.globalMaxSize, minimum = 1),
            perAppMaxSize = values.int(KEY_QUEUE_PER_APP_MAX, defaults.perAppMaxSize, minimum = 1),
            overflowPolicy = values.enum(KEY_QUEUE_OVERFLOW, defaults.overflowPolicy),
            repeatNotificationUpdates = values.boolean(KEY_QUEUE_REPEAT, defaults.repeatNotificationUpdates),
        )
    }

    private fun decodeFormat(values: Map<String, String>): AnnouncementFormat {
        val defaults = AnnouncementFormat()
        return AnnouncementFormat(
            includeAppName = values.boolean(KEY_FORMAT_APP, defaults.includeAppName),
            includeSender = values.boolean(KEY_FORMAT_SENDER, defaults.includeSender),
            includeMessage = values.boolean(KEY_FORMAT_MESSAGE, defaults.includeMessage),
            introduction = values[KEY_FORMAT_INTRO],
            pauseBetweenSectionsMillis = values.long(
                KEY_FORMAT_PAUSE,
                defaults.pauseBetweenSectionsMillis,
                minimum = 0L,
            ),
        )
    }

    private fun decodeSpeech(values: Map<String, String>): SpeechSettings {
        val defaults = SpeechSettings()
        val parsedVolume = values[KEY_SPEECH_VOLUME]?.toFloatOrNull()?.coerceIn(0f, 1f) ?: defaults.volumeLevel
        return SpeechSettings(
            speechRate = values.positiveFloat(KEY_SPEECH_RATE, defaults.speechRate),
            pitch = values.positiveFloat(KEY_SPEECH_PITCH, defaults.pitch),
            voiceId = values[KEY_SPEECH_VOICE]?.takeIf { it.isNotBlank() },
            volumeLevel = parsedVolume,
        )
    }

    private fun decodeHistory(values: Map<String, String>): HistorySettings {
        val defaults = HistorySettings()
        return HistorySettings(
            storageMode = values.enum(KEY_HISTORY_MODE, defaults.storageMode),
            updateHandling = values.enum(KEY_HISTORY_UPDATES, defaults.updateHandling),
            retentionDays = values[KEY_HISTORY_RETENTION]?.toIntOrNull()?.takeIf { it >= 0 },
            requireAuth = values.boolean(KEY_HISTORY_AUTH, defaults.requireAuth),
        )
    }

    private fun decodeSecurity(values: Map<String, String>): SecuritySettings {
        val defaults = SecuritySettings()
        return SecuritySettings(
            biometricLockEnabled = values.boolean(KEY_SECURITY_BIOMETRIC, defaults.biometricLockEnabled),
            secureScreenEnabled = values.boolean(KEY_SECURITY_FLAG_SECURE, defaults.secureScreenEnabled),
        )
    }

    private fun decodeOverrides(values: Map<String, String>): Map<String, AppOverride> {
        val packageNames = LinkedHashSet<String>()
        values.keys.forEach { key ->
            if (!key.startsWith(APP_PREFIX)) return@forEach
            val parts = key.split(':', limit = 3)
            if (parts.size == 3 && parts[1].isNotBlank()) packageNames.add(parts[1])
        }
        return packageNames.associateWith { packageName -> decodeOverride(packageName, values) }
    }

    private fun decodeOverride(packageName: String, values: Map<String, String>): AppOverride {
        val prefix = "$APP_PREFIX$packageName:"
        fun string(key: String): String? = values[prefix + key]

        // Whole groups are present when their marker key is present (the encoder always
        // writes the marker for a non-null group).
        val lengthPresent = string(APP_LENGTH_SCOPE) != null
        val keywordsPresent = string(APP_KEYWORDS_ENABLED) != null
        val privacyPresent = string(APP_PRIVACY_EXCLUDE) != null
        val audioPresent = string(APP_AUDIO_WIRED) != null

        return AppOverride(
            eligibility = AppEligibilityOverride(
                screenCondition = string("elig.screen")?.enumOrNull<ScreenCondition>(),
                lockCondition = string("elig.lock")?.enumOrNull<LockCondition>(),
                respectDnd = string("elig.respectDnd")?.toBooleanStrictOrNull(),
                queueDuringCall = string("elig.queueDuringCall")?.toBooleanStrictOrNull(),
                audioFocus = string("elig.audioFocus")?.enumOrNull<AudioFocusPolicy>(),
                textLength = if (lengthPresent) {
                    TextLengthSettings(
                        maxLengthChars = string(APP_LENGTH_MAX)?.toIntOrNull()?.takeIf { it > 0 },
                        scope = string(APP_LENGTH_SCOPE)?.enumOrNull<LengthScope>()
                            ?: LengthScope.MESSAGE,
                    )
                } else {
                    null
                },
                keywordFilter = if (keywordsPresent) {
                    KeywordFilterSettings(
                        enabled = string(APP_KEYWORDS_ENABLED)?.toBooleanStrictOrNull() ?: false,
                        keywords = splitList(string(APP_KEYWORDS_LIST)),
                        mode = string(APP_KEYWORDS_MODE)?.enumOrNull<KeywordMatchMode>()
                            ?: KeywordMatchMode.WHOLE_WORD,
                    )
                } else {
                    null
                },
                privacy = if (privacyPresent) {
                    PrivacySettings(
                        excludeApp = string(APP_PRIVACY_EXCLUDE)?.toBooleanStrictOrNull() ?: false,
                        skipOtp = string(APP_PRIVACY_OTP)?.toBooleanStrictOrNull() ?: true,
                        skipBanking = string(APP_PRIVACY_BANKING)?.toBooleanStrictOrNull() ?: false,
                        skipPassword = string(APP_PRIVACY_PASSWORD)?.toBooleanStrictOrNull() ?: true,
                        skipPayment = string(APP_PRIVACY_PAYMENT)?.toBooleanStrictOrNull() ?: false,
                        hideContent = string(APP_PRIVACY_HIDE)?.toBooleanStrictOrNull() ?: false,
                        sensitiveKeywords = splitList(string(APP_PRIVACY_KEYWORDS)),
                    )
                } else {
                    null
                },
                audioOutput = if (audioPresent) {
                    AudioOutputSettings(
                        allowWired = string(APP_AUDIO_WIRED)?.toBooleanStrictOrNull() ?: true,
                        allowBluetooth = string(APP_AUDIO_BLUETOOTH)?.toBooleanStrictOrNull() ?: true,
                        allowPhoneSpeaker = string(APP_AUDIO_SPEAKER)?.toBooleanStrictOrNull() ?: false,
                        allowedBluetoothIds = string(APP_AUDIO_BT_IDS)?.let { raw ->
                            splitList(raw).filter { it.isNotBlank() }.toSet()
                        },
                        allowUnidentifiedOutput = string(APP_AUDIO_UNIDENTIFIED)
                            ?.toBooleanStrictOrNull() ?: true,
                    )
                } else {
                    null
                },
            ),
            queue = AppQueueOverride(
                groupingWindowMillis = string(APP_QUEUE_WINDOW)?.toLongOrNull()?.takeIf { it >= 0L },
            ),
            history = AppHistoryOverride(
                storageMode = string(APP_HISTORY_MODE)?.enumOrNull<HistoryStorageMode>(),
                updateHandling = string(APP_HISTORY_UPDATES)?.enumOrNull<HistoryUpdateHandling>(),
                retentionDays = string(APP_HISTORY_RETENTION)?.toIntOrNull(),
                requireAuth = string(APP_HISTORY_AUTH)?.toBooleanStrictOrNull(),
            ),
            priority = string(APP_PRIORITY)?.enumOrNull<AppPriority>(),
            spokenName = string(APP_SPOKEN_NAME)?.takeIf { it.isNotBlank() },
            categoryOverride = string(APP_CATEGORY)?.enumOrNull<AppCategoryChoice>(),
        )
    }

    /**
     * Encodes remembered devices as `id\u001Elabel\u001EisBluetooth` entries.
     *
     * Both separators are stripped from the values, so a device name containing either
     * character can never corrupt the list.
     */
    private fun encodeDevices(devices: List<KnownAudioDevice>): String = devices.map { device ->
        listOf(
            device.id.sanitiseSeparators(),
            device.label.sanitiseSeparators(),
            device.isBluetooth.toString(),
        ).joinToString(FIELD_SEPARATOR)
    }.joinToString(LIST_SEPARATOR)

    /** Decodes remembered devices, skipping any entry that is not exactly three fields. */
    private fun decodeDevices(raw: String?): List<KnownAudioDevice> =
        splitList(raw).mapNotNull { entry ->
            val fields = entry.split(FIELD_SEPARATOR)
            if (fields.size != 3 || fields[0].isBlank()) return@mapNotNull null
            KnownAudioDevice(
                id = fields[0],
                label = fields[1].ifBlank { fields[0] },
                isBluetooth = fields[2].toBooleanStrictOrNull() ?: true,
            )
        }.distinctBy { it.id }

    private fun String.sanitiseSeparators(): String =
        replace(LIST_SEPARATOR, " ").replace(FIELD_SEPARATOR, " ")

    private fun encodeList(items: Collection<String>): String =
        items.map { it.replace(LIST_SEPARATOR, " ") }.joinToString(LIST_SEPARATOR)

    private fun splitList(raw: String?): List<String> =
        raw?.takeIf { it.isNotEmpty() }?.split(LIST_SEPARATOR) ?: emptyList()

    private fun Map<String, String>.boolean(key: String, default: Boolean): Boolean =
        this[key]?.toBooleanStrictOrNull() ?: default

    private fun Map<String, String>.long(key: String, default: Long, minimum: Long): Long =
        this[key]?.toLongOrNull()?.takeIf { it >= minimum } ?: default

    private fun Map<String, String>.int(key: String, default: Int, minimum: Int): Int =
        this[key]?.toIntOrNull()?.takeIf { it >= minimum } ?: default

    private fun Map<String, String>.positiveFloat(key: String, default: Float): Float =
        this[key]?.toFloatOrNull()?.takeIf { it > 0f && it.isFinite() } ?: default

    private inline fun <reified T : Enum<T>> Map<String, String>.enum(key: String, default: T): T =
        this[key]?.enumOrNull<T>() ?: default

    private inline fun <reified T : Enum<T>> String?.enumOrNull(): T? =
        this?.let { value -> enumValues<T>().firstOrNull { it.name == value } }
}
