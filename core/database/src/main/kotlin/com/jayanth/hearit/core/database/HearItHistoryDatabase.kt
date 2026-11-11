package com.jayanth.hearit.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * The app's only database: history entries (master prompt §12, phase P10).
 *
 * Schema policy (ADR-022): `exportSchema = true` (schemas are committed under
 * `core/database/schemas`), and every schema change **bumps [version] and adds a
 * [androidx.room.migration.Migration] with a migration test**. There is deliberately no
 * `fallbackToDestructiveMigration()` — history is sensitive user data and must never be
 * dropped silently.
 */
@Database(
    entities = [HistoryEntryEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class HearItHistoryDatabase : RoomDatabase() {

    /** History table access. */
    abstract fun historyDao(): HistoryDao
}

/** Construction entry point; one database instance per process. */
object HistoryDatabase {

    /** File name of the history database. */
    const val FILE_NAME: String = "hearit_history.db"

    /** Builds the database; callers hold the single instance (e.g. in the Hilt graph). */
    fun create(context: Context): HearItHistoryDatabase = Room.databaseBuilder(
        context.applicationContext,
        HearItHistoryDatabase::class.java,
        FILE_NAME,
    ).build()
}
