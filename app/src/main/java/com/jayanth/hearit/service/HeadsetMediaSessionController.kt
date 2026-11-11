package com.jayanth.hearit.service

import android.content.Context
import android.content.Intent
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import com.jayanth.hearit.core.common.model.HeadsetButtonAction

/**
 * Manages the scoped Bluetooth Headset [MediaSession] while HearIt is speaking or paused mid-utterance.
 *
 * Keeping [MediaSession.isActive] false whenever idle guarantees music apps like Spotify or
 * YouTube Music retain exclusive headset button control between announcements.
 */
class HeadsetMediaSessionController(
    context: Context,
    private val sessionStore: SessionStore,
    private val onAction: (HeadsetButtonAction) -> Unit,
) {
    private val handler = Handler(Looper.getMainLooper())
    private val mediaSession = MediaSession(context.applicationContext, "HearItMediaSession")
    private var tapCount = 0

    private val tapFlushRunnable = Runnable {
        val count = tapCount
        tapCount = 0
        val configuredAction = when {
            count >= 3 -> sessionStore.headsetTriplePressAction
            count == 2 -> sessionStore.headsetDoublePressAction
            count == 1 -> sessionStore.headsetSinglePressAction
            else -> HeadsetButtonAction.NONE
        }
        if (configuredAction != HeadsetButtonAction.NONE) {
            onAction(configuredAction)
        }
    }

    init {
        mediaSession.setCallback(
            object : MediaSession.Callback() {
                override fun onMediaButtonEvent(mediaButtonIntent: Intent): Boolean {
                    if (!sessionStore.isHeadsetMediaControlsEnabled) return false
                    @Suppress("DEPRECATION")
                    val keyEvent: KeyEvent = mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
                        ?: return super.onMediaButtonEvent(mediaButtonIntent)
                    if (keyEvent.action != KeyEvent.ACTION_DOWN) return true

                    return when (keyEvent.keyCode) {
                        KeyEvent.KEYCODE_HEADSETHOOK,
                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                            tapCount++
                            handler.removeCallbacks(tapFlushRunnable)
                            handler.postDelayed(tapFlushRunnable, MULTI_TAP_WINDOW_MS)
                            true
                        }
                        KeyEvent.KEYCODE_MEDIA_PLAY,
                        KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                            dispatchConfigured(sessionStore.headsetSinglePressAction)
                            true
                        }
                        KeyEvent.KEYCODE_MEDIA_NEXT -> {
                            dispatchConfigured(sessionStore.headsetDoublePressAction)
                            true
                        }
                        KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                            dispatchConfigured(sessionStore.headsetTriplePressAction)
                            true
                        }
                        else -> super.onMediaButtonEvent(mediaButtonIntent)
                    }
                }

                override fun onPlay() {
                    dispatchConfigured(sessionStore.headsetSinglePressAction)
                }

                override fun onPause() {
                    dispatchConfigured(sessionStore.headsetSinglePressAction)
                }

                override fun onSkipToNext() {
                    dispatchConfigured(sessionStore.headsetDoublePressAction)
                }

                override fun onSkipToPrevious() {
                    dispatchConfigured(sessionStore.headsetTriplePressAction)
                }
            },
            handler,
        )
        mediaSession.isActive = false
    }

    private fun dispatchConfigured(action: HeadsetButtonAction) {
        if (!sessionStore.isHeadsetMediaControlsEnabled || action == HeadsetButtonAction.NONE) return
        onAction(action)
    }

    fun updateState(isSpeaking: Boolean, isPausedMidUtterance: Boolean) {
        val shouldActivate = sessionStore.isHeadsetMediaControlsEnabled && (isSpeaking || isPausedMidUtterance)
        val stateCode = when {
            isSpeaking -> PlaybackState.STATE_PLAYING
            isPausedMidUtterance -> PlaybackState.STATE_PAUSED
            else -> PlaybackState.STATE_NONE
        }
        val state = PlaybackState.Builder()
            .setActions(
                PlaybackState.ACTION_PLAY_PAUSE or
                    PlaybackState.ACTION_PLAY or
                    PlaybackState.ACTION_PAUSE or
                    PlaybackState.ACTION_SKIP_TO_NEXT or
                    PlaybackState.ACTION_SKIP_TO_PREVIOUS,
            )
            .setState(stateCode, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1.0f)
            .build()
        mediaSession.setPlaybackState(state)
        mediaSession.isActive = shouldActivate
    }

    fun release() {
        handler.removeCallbacks(tapFlushRunnable)
        tapCount = 0
        mediaSession.isActive = false
        mediaSession.release()
    }

    private companion object {
        const val MULTI_TAP_WINDOW_MS = 350L
    }
}
