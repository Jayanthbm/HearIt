package com.jayanth.hearit.service

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * Privacy-first on-device instantaneous ambient sound level sampler.
 *
 * Samples ~120ms of mono PCM samples in RAM to estimate ambient loudness in dB SPL and
 * immediately discards the buffer — audio is never saved, buffered longer, or transmitted.
 */
object AmbientNoiseSampler {

    fun hasRecordAudioPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Converts raw 16-bit PCM RMS (`0.0..32767.0`) into an approximate dB SPL estimate.
     * Calibrated cleanly so typical quiet room ~45 dB and busy street/subway >=70 dB.
     */
    fun computeDbSplFromRms(rms: Double): Double {
        if (rms <= 1.0) return 20.0
        val dbFs = 20.0 * log10(rms / 32767.0)
        return (dbFs + 96.0).coerceIn(20.0, 110.0)
    }

    /** Synchronously samples ~120ms off calling thread and returns estimated dB SPL, or null if unavailable. */
    @SuppressLint("MissingPermission")
    fun measureAmbientDb(context: Context): Double? {
        if (!hasRecordAudioPermission(context)) return null
        val sampleRate = 16_000
        val minBuf = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minBuf <= 0) return null

        val targetSamples = (sampleRate * 120) / 1000
        val buffer = ShortArray(targetSamples)
        var recorder: AudioRecord? = null
        return try {
            recorder = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                maxOf(minBuf, targetSamples * 2),
            )
            if (recorder.state != AudioRecord.STATE_INITIALIZED) return null
            recorder.startRecording()
            val read = recorder.read(buffer, 0, targetSamples)
            if (read <= 0) return null
            var sumSq = 0.0
            for (i in 0 until read) {
                val sample = buffer[i].toDouble()
                sumSq += sample * sample
            }
            val rms = sqrt(sumSq / read.toDouble())
            computeDbSplFromRms(rms)
        } catch (_: Throwable) {
            null
        } finally {
            try {
                recorder?.stop()
            } catch (_: Throwable) {}
            try {
                recorder?.release()
            } catch (_: Throwable) {}
        }
    }
}
