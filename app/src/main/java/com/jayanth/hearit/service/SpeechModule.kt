package com.jayanth.hearit.service

import android.content.Context
import android.media.AudioManager
import com.jayanth.hearit.core.tts.TextToSpeechEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency-injection wiring for the announcement service (ADR-001: Hilt, constructor
 * injection, no service locator).
 *
 * Both bindings live for the whole process: the TTS engine is expensive to initialise and
 * must be shared by anything that speaks; [AudioManager] is a framework singleton.
 */
@Module
@InstallIn(SingletonComponent::class)
object SpeechModule {

    @Provides
    @Singleton
    fun textToSpeechEngine(@ApplicationContext context: Context): TextToSpeechEngine =
        AndroidTextToSpeechEngine(context)

    @Provides
    @Singleton
    fun audioManager(@ApplicationContext context: Context): AudioManager =
        context.getSystemService(AudioManager::class.java)
}
