package com.jayanth.hearit.di

import android.content.Context
import com.jayanth.hearit.core.datastore.HearItSettingsDataStore
import com.jayanth.hearit.core.datastore.HearItSetupDataStore
import com.jayanth.hearit.core.datastore.SettingsRepository
import com.jayanth.hearit.core.datastore.SetupStateRepository
import com.jayanth.hearit.service.SessionStore
import com.jayanth.hearit.service.SettingsSource
import com.jayanth.hearit.setup.AndroidAppCatalogProbe
import com.jayanth.hearit.setup.AndroidAudioOutputProbe
import com.jayanth.hearit.setup.AndroidBluetoothPermissionProbe
import com.jayanth.hearit.setup.AndroidNotificationAccessProbe
import com.jayanth.hearit.setup.AndroidPostNotificationProbe
import com.jayanth.hearit.setup.AndroidSetupLauncher
import com.jayanth.hearit.setup.AppCatalogProbe
import com.jayanth.hearit.setup.AudioOutputProbe
import com.jayanth.hearit.setup.BluetoothPermissionProbe
import com.jayanth.hearit.setup.NotificationAccessProbe
import com.jayanth.hearit.setup.PostNotificationProbe
import com.jayanth.hearit.setup.SetupLauncher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Storage and setup-platform bindings (P11, ADR-023).
 *
 * Kept in `:app` because it is the only module that may see the framework-typed probes and
 * the announcement service; `:core:*` stays Hilt-free so each module remains independently
 * testable. All bindings are process singletons: both DataStore files and the session store
 * must exist once per process.
 */
@Module
@InstallIn(SingletonComponent::class)
object SetupModule {

    @Provides
    @Singleton
    fun settingsRepository(@ApplicationContext context: Context): SettingsRepository =
        HearItSettingsDataStore.repository(context)

    /**
     * One settings source per process: it mirrors the repository flow into a snapshot that
     * the announcement service and (later) other pipeline consumers read synchronously.
     */
    @Provides
    @Singleton
    fun settingsSource(repository: SettingsRepository): SettingsSource =
        SettingsSource(repository)

    @Provides
    @Singleton
    fun setupStateRepository(@ApplicationContext context: Context): SetupStateRepository =
        HearItSetupDataStore.repository(context)

    @Provides
    @Singleton
    fun sessionStore(@ApplicationContext context: Context): SessionStore = SessionStore(context)

    @Provides
    @Singleton
    fun notificationAccessProbe(@ApplicationContext context: Context): NotificationAccessProbe =
        AndroidNotificationAccessProbe(context)

    @Provides
    @Singleton
    fun postNotificationProbe(@ApplicationContext context: Context): PostNotificationProbe =
        AndroidPostNotificationProbe(context)

    @Provides
    @Singleton
    fun bluetoothPermissionProbe(@ApplicationContext context: Context): BluetoothPermissionProbe =
        AndroidBluetoothPermissionProbe(context)

    @Provides
    @Singleton
    fun appCatalogProbe(@ApplicationContext context: Context): AppCatalogProbe =
        AndroidAppCatalogProbe(context)

    @Provides
    @Singleton
    fun audioOutputProbe(@ApplicationContext context: Context): AudioOutputProbe =
        AndroidAudioOutputProbe(context)

    @Provides
    @Singleton
    fun setupLauncher(@ApplicationContext context: Context): SetupLauncher =
        AndroidSetupLauncher(context)
}
