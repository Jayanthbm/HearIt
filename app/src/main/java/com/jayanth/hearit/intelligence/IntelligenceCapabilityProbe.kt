package com.jayanth.hearit.intelligence

import android.content.Context
import android.os.Build
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.entityextraction.Entity
import com.google.mlkit.nl.entityextraction.EntityExtraction
import com.google.mlkit.nl.entityextraction.EntityExtractionParams
import com.google.mlkit.nl.entityextraction.EntityExtractor
import com.google.mlkit.nl.entityextraction.EntityExtractorOptions
import com.jayanth.hearit.core.tts.BuiltAnnouncement
import com.jayanth.hearit.core.tts.SectionKind
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

enum class MlKitModelStatus {
    READY,
    DOWNLOAD_REQUIRED,
    PLAY_SERVICES_UNAVAILABLE,
}

data class LocalAiCapabilityReport(
    val mlKitStatus: MlKitModelStatus,
    val mlKitStatusLabel: String,
) {
    val mlKitSupported: Boolean get() = mlKitStatus != MlKitModelStatus.PLAY_SERVICES_UNAVAILABLE
    val mlKitModelReady: Boolean get() = mlKitStatus == MlKitModelStatus.READY
}

/**
 * Pure JVM helper for Tier 2 entity-to-speech formatting (#15).
 */
object LocalAiEnhancer {

    /**
     * Expands detected tracking IDs, flight numbers, and IBAN codes cleanly so native TTS
     * articulates characters instead of mispronouncing codes as words or thousands.
     */
    fun formatStructuredEntities(
        text: String,
        trackingNumbers: List<String> = emptyList(),
        flightNumbers: List<String> = emptyList(),
        ibans: List<String> = emptyList(),
    ): String {
        var result = text
        trackingNumbers.filter { it.length >= 4 }.distinct().forEach { code ->
            val spoken = "tracking code ${spellAlphanumeric(code)}"
            result = result.replace(code, spoken)
        }
        flightNumbers.filter { it.length >= 3 }.distinct().forEach { flight ->
            val spoken = "flight ${spellAlphanumeric(flight)}"
            if (!result.contains("flight ${flight}", ignoreCase = true)) {
                result = result.replace(flight, spoken)
            } else {
                result = result.replace(flight, spellAlphanumeric(flight))
            }
        }
        ibans.filter { it.length >= 8 }.distinct().forEach { iban ->
            result = result.replace(iban, "account ${spellAlphanumeric(iban)}")
        }
        return result
    }

    internal fun spellAlphanumeric(raw: String): String {
        val sb = StringBuilder()
        var digitBuffer = StringBuilder()
        fun flushDigits() {
            if (digitBuffer.isNotEmpty()) {
                if (sb.isNotEmpty()) sb.append(' ')
                sb.append(digitBuffer.toString().toCharArray().joinToString(" "))
                digitBuffer = StringBuilder()
            }
        }
        for (ch in raw.trim()) {
            when {
                ch.isLetter() -> {
                    flushDigits()
                    if (sb.isNotEmpty()) sb.append(' ')
                    sb.append(ch.uppercaseChar())
                }
                ch.isDigit() -> digitBuffer.append(ch)
                else -> flushDigits()
            }
        }
        flushDigits()
        return sb.toString()
    }
}

/**
 * Probes Tier 2 local AI capabilities (#15):
 *  - ML Kit Entity Extraction (lightweight on-demand Play Services model)
 */
@Singleton
class IntelligenceCapabilityProbe @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    @Volatile
    private var cachedMlKitReady: Boolean = false

    private val extractor: EntityExtractor by lazy {
        EntityExtraction.getClient(
            EntityExtractorOptions.Builder(EntityExtractorOptions.ENGLISH).build(),
        )
    }

    suspend fun probe(): LocalAiCapabilityReport = withContext(Dispatchers.IO) {
        val playServicesOk = runCatching {
            GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
        }.getOrDefault(false)

        val (mlKitStatus, mlKitLabel) = if (!playServicesOk) {
            cachedMlKitReady = false
            MlKitModelStatus.PLAY_SERVICES_UNAVAILABLE to "Play Services Required"
        } else {
            val downloaded = runCatching {
                Tasks.await(extractor.isModelDownloaded, 2_500L, TimeUnit.MILLISECONDS)
            }.getOrNull()
            when (downloaded) {
                true -> {
                    cachedMlKitReady = true
                    MlKitModelStatus.READY to "Supported · Ready"
                }
                false -> {
                    cachedMlKitReady = false
                    MlKitModelStatus.DOWNLOAD_REQUIRED to "Supported · Model Not Downloaded"
                }
                null -> {
                    cachedMlKitReady = false
                    MlKitModelStatus.PLAY_SERVICES_UNAVAILABLE to "Status Unavailable"
                }
            }
        }

        LocalAiCapabilityReport(
            mlKitStatus = mlKitStatus,
            mlKitStatusLabel = mlKitLabel,
        )
    }

    suspend fun downloadMlKitModel(): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val conditions = DownloadConditions.Builder().build()
            Tasks.await(extractor.downloadModelIfNeeded(conditions), 45_000L, TimeUnit.MILLISECONDS)
            val ready = Tasks.await(extractor.isModelDownloaded, 2_500L, TimeUnit.MILLISECONDS)
            cachedMlKitReady = ready
            ready
        }
    }

    /**
     * Runs bounded Tier 2 entity enhancement over message sections without exceeding 180ms.
     * Falls back seamlessly to Tier 1 if model is unavailable or times out.
     */
    suspend fun enhanceAnnouncement(
        announcement: BuiltAnnouncement,
        mlKitEnabled: Boolean,
    ): BuiltAnnouncement {
        if (!mlKitEnabled || !cachedMlKitReady) return announcement
        return withTimeoutOrNull(180L) {
            withContext(Dispatchers.Default) {
                val updatedSections = announcement.sections.map { section ->
                    if (section.kind != SectionKind.MESSAGE || section.text.length < 16) {
                        section
                    } else {
                        val annotations = runCatching {
                            Tasks.await(
                                extractor.annotate(EntityExtractionParams.Builder(section.text).build()),
                                140L,
                                TimeUnit.MILLISECONDS,
                            )
                        }.getOrNull().orEmpty()

                        if (annotations.isEmpty()) {
                            section
                        } else {
                            val tracking = mutableListOf<String>()
                            val flights = mutableListOf<String>()
                            val ibans = mutableListOf<String>()
                            annotations.forEach { ann ->
                                ann.entities.forEach { entity ->
                                    when (entity.type) {
                                        Entity.TYPE_TRACKING_NUMBER -> tracking += ann.annotatedText
                                        Entity.TYPE_FLIGHT_NUMBER -> flights += ann.annotatedText
                                        Entity.TYPE_IBAN -> ibans += ann.annotatedText
                                    }
                                }
                            }
                            if (tracking.isEmpty() && flights.isEmpty() && ibans.isEmpty()) {
                                section
                            } else {
                                section.copy(
                                    text = LocalAiEnhancer.formatStructuredEntities(
                                        text = section.text,
                                        trackingNumbers = tracking,
                                        flightNumbers = flights,
                                        ibans = ibans,
                                    ),
                                )
                            }
                        }
                    }
                }
                announcement.copy(sections = updatedSections)
            }
        } ?: announcement
    }
}
