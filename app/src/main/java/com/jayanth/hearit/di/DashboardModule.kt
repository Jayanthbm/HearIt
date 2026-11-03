package com.jayanth.hearit.di

import android.content.Context
import com.jayanth.hearit.core.common.time.Clock
import com.jayanth.hearit.core.database.HearItHistoryDatabase
import com.jayanth.hearit.core.database.HistoryDatabase
import com.jayanth.hearit.core.database.HistoryRepository
import com.jayanth.hearit.core.database.RoomHistoryRepository
import com.jayanth.hearit.dashboard.AndroidServiceControl
import com.jayanth.hearit.dashboard.ServiceControl
import com.jayanth.hearit.dashboard.ServiceStatusMonitor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dashboard and history-storage bindings (P12, ADR-024).
 *
 * [ServiceStatusMonitor] must be a process singleton: the service writes it and the
 * dashboard reads it, so two instances would silently disconnect the two.
 *
 * History is recorded by `AnnouncementService` from the pipeline's events (L3), so the
 * dashboard and history feature read real rows; the dedicated History screen (P18) is still
 * pending.
 */
@Module
@InstallIn(SingletonComponent::class)
object DashboardModule {

    @Provides
    @Singleton
    fun serviceStatusMonitor(): ServiceStatusMonitor = ServiceStatusMonitor()

    @Provides
    @Singleton
    fun serviceControl(@ApplicationContext context: Context): ServiceControl =
        AndroidServiceControl(context)

    @Provides
    @Singleton
    fun historyDatabase(@ApplicationContext context: Context): HearItHistoryDatabase =
        HistoryDatabase.create(context)

    /**
     * History uses a wall clock (not the queue's monotonic one): retention and "recorded
     * at" are user-visible calendar times, and `Clock` is the injected seam for both.
     */
    @Provides
    @Singleton
    fun historyRepository(database: HearItHistoryDatabase): HistoryRepository =
        RoomHistoryRepository(
            dao = database.historyDao(),
            clock = Clock { System.currentTimeMillis() },
        )
}
