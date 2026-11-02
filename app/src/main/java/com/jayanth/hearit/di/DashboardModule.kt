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
 * The history database is provided here for the first time; history recording itself is
 * still unwired (P18), so the dashboard reads an empty table on a fresh install and the
 * section says so.
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
