package com.jayanth.hearit.diagnostics

import android.app.Notification
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Bundle
import android.os.Parcelable
import android.service.notification.StatusBarNotification
import com.jayanth.hearit.core.common.logger.HearItLogger
import com.jayanth.hearit.core.common.logger.LogCategory
import com.jayanth.hearit.core.common.model.NotificationSnapshot
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.OutputStream
import java.io.PrintWriter
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONArray
import org.json.JSONObject

/**
 * High-fidelity raw notification recorder for the 2-day deep diagnostic study.
 *
 * Captures 100% of raw platform StatusBarNotification metadata, Notification extras,
 * and MessagingStyle bundles for target apps with ZERO filters before any pipeline
 * decision is made.
 */
@Singleton
class RawNotificationLogger @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    companion object {
        const val DB_NAME = "hearit_raw_debug.db"
        const val DB_VERSION = 1
        const val TABLE_RAW = "raw_notifications"

        val TARGET_PACKAGES = setOf(
            "com.whatsapp",
            "com.microsoft.teams",
            "com.google.android.apps.messaging",
            "com.samsung.android.messaging",
            "in.cricketexchange.app.cricketexchange",
            "com.instagram.android",
            "com.instagram.barcelona",
        )
    }

    private val dbHelper = object : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS $TABLE_RAW (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    recorded_at_millis INTEGER NOT NULL,
                    post_time_millis INTEGER NOT NULL,
                    package_name TEXT NOT NULL,
                    sbn_key TEXT NOT NULL,
                    sbn_id INTEGER NOT NULL,
                    sbn_tag TEXT,
                    group_key TEXT,
                    is_group INTEGER NOT NULL,
                    is_group_summary INTEGER NOT NULL,
                    is_ongoing INTEGER NOT NULL,
                    is_clearable INTEGER NOT NULL,
                    flags INTEGER NOT NULL,
                    channel_id TEXT,
                    category TEXT,
                    ticker TEXT,
                    android_title TEXT,
                    android_text TEXT,
                    android_big_text TEXT,
                    android_sub_text TEXT,
                    android_summary_text TEXT,
                    android_text_lines TEXT,
                    android_messages_json TEXT,
                    all_extras_json TEXT,
                    extracted_sender TEXT,
                    extracted_message_text TEXT,
                    extracted_group_title TEXT,
                    extracted_is_group INTEGER,
                    extracted_is_group_summary INTEGER,
                    raw_sbn_dump TEXT
                );
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_raw_pkg_time ON $TABLE_RAW(package_name, recorded_at_millis);")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_raw_sbn_key ON $TABLE_RAW(sbn_key);")
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            // Debug diagnostic database: recreated if version changes
            db.execSQL("DROP TABLE IF EXISTS $TABLE_RAW")
            onCreate(db)
        }
    }

    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "HearIt-RawNotificationLogger").apply { isDaemon = true }
    }

    private val sessionStore by lazy { com.jayanth.hearit.service.SessionStore(context) }

    fun isTarget(packageName: String): Boolean = packageName in sessionStore.notificationStudyPackages

    fun log(sbn: StatusBarNotification, snapshot: NotificationSnapshot) {
        if (!sessionStore.isNotificationStudyEnabled) return
        if (!isTarget(sbn.packageName)) return

        val now = System.currentTimeMillis()
        executor.execute {
            try {
                val db = dbHelper.writableDatabase
                val notif = sbn.notification
                val extras = notif.extras ?: Bundle()

                val isGroupSummary = if ((notif.flags and Notification.FLAG_GROUP_SUMMARY) != 0) 1 else 0
                val isGroup = if (sbn.isGroup) 1 else 0
                val isOngoing = if (sbn.isOngoing) 1 else 0
                val isClearable = if (sbn.isClearable) 1 else 0

                val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
                val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
                val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
                val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
                val summaryText = extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString()
                val textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
                    ?.joinToString("\n") { it.toString() }

                val messagesJson = extractMessagesJson(extras)
                val allExtrasJson = extractAllExtrasJson(extras)

                val values = ContentValues().apply {
                    put("recorded_at_millis", now)
                    put("post_time_millis", sbn.postTime)
                    put("package_name", sbn.packageName)
                    put("sbn_key", sbn.key)
                    put("sbn_id", sbn.id)
                    put("sbn_tag", sbn.tag)
                    put("group_key", sbn.groupKey)
                    put("is_group", isGroup)
                    put("is_group_summary", isGroupSummary)
                    put("is_ongoing", isOngoing)
                    put("is_clearable", isClearable)
                    put("flags", notif.flags)
                    put("channel_id", notif.channelId)
                    put("category", notif.category)
                    put("ticker", notif.tickerText?.toString())
                    put("android_title", title)
                    put("android_text", text)
                    put("android_big_text", bigText)
                    put("android_sub_text", subText)
                    put("android_summary_text", summaryText)
                    put("android_text_lines", textLines)
                    put("android_messages_json", messagesJson)
                    put("all_extras_json", allExtrasJson)
                    put("extracted_sender", snapshot.sender)
                    put("extracted_message_text", snapshot.messageText)
                    put("extracted_group_title", snapshot.groupTitle)
                    put("extracted_is_group", if (snapshot.isGroup) 1 else 0)
                    put("extracted_is_group_summary", if (snapshot.isGroupSummary) 1 else 0)
                    put("raw_sbn_dump", sbn.toString())
                }

                db.insert(TABLE_RAW, null, values)
            } catch (e: Exception) {
                HearItLogger.warn(
                    LogCategory.NOTIFICATION,
                    "RawNotificationLogger failed for ${sbn.packageName}: ${e.message}",
                )
            }
        }
    }

    private fun extractMessagesJson(extras: Bundle): String {
        return runCatching {
            @Suppress("DEPRECATION")
            val parcelables = extras.getParcelableArray(Notification.EXTRA_MESSAGES) ?: return ""
            val array = JSONArray()
            for (p in parcelables) {
                if (p is Bundle) {
                    val obj = JSONObject()
                    obj.put("text", p.getCharSequence("text")?.toString() ?: "")
                    obj.put("time", p.getLong("time", 0L))
                    val sender = p.getCharSequence("sender")?.toString()
                    if (sender != null) obj.put("sender", sender)
                    val senderPerson = p.getBundle("sender_person")
                    if (senderPerson != null) {
                        obj.put("sender_person_name", senderPerson.getCharSequence("name")?.toString() ?: "")
                    }
                    array.put(obj)
                }
            }
            array.toString()
        }.getOrDefault("")
    }

    private fun extractAllExtrasJson(extras: Bundle): String {
        return runCatching {
            val json = JSONObject()
            for (key in extras.keySet()) {
                val value = extras.get(key)
                when (value) {
                    null -> json.put(key, JSONObject.NULL)
                    is Number, is Boolean, is String -> json.put(key, value)
                    is CharSequence -> json.put(key, value.toString())
                    is Array<*> -> {
                        val arr = JSONArray()
                        value.take(20).forEach { arr.put(it?.toString() ?: "") }
                        json.put(key, arr)
                    }
                    is List<*> -> {
                        val arr = JSONArray()
                        value.take(20).forEach { arr.put(it?.toString() ?: "") }
                        json.put(key, arr)
                    }
                    is Bundle -> {
                        json.put(key, "[Nested Bundle with ${value.size()} keys]")
                    }
                    else -> json.put(key, value.toString())
                }
            }
            json.toString()
        }.getOrDefault("{}")
    }

    fun getTotalCount(): Long {
        return runCatching {
            val db = dbHelper.readableDatabase
            db.rawQuery("SELECT COUNT(*) FROM $TABLE_RAW", null).use { cursor ->
                if (cursor.moveToFirst()) cursor.getLong(0) else 0L
            }
        }.getOrDefault(0L)
    }

    fun getCountsByPackage(): Map<String, Long> {
        return runCatching {
            val db = dbHelper.readableDatabase
            val map = mutableMapOf<String, Long>()
            db.rawQuery("SELECT package_name, COUNT(*) FROM $TABLE_RAW GROUP BY package_name", null).use { cursor ->
                while (cursor.moveToNext()) {
                    map[cursor.getString(0)] = cursor.getLong(1)
                }
            }
            map
        }.getOrDefault(emptyMap())
    }

    fun clearAll() {
        executor.execute {
            runCatching {
                val db = dbHelper.writableDatabase
                db.execSQL("DELETE FROM $TABLE_RAW")
            }
        }
    }

    fun exportToJson(outputStream: OutputStream) {
        val db = dbHelper.readableDatabase
        val writer = PrintWriter(outputStream.bufferedWriter(Charsets.UTF_8))
        try {
            writer.println("[")
            db.rawQuery("SELECT * FROM $TABLE_RAW ORDER BY recorded_at_millis ASC", null).use { cursor ->
                var first = true
                val colNames = cursor.columnNames
                while (cursor.moveToNext()) {
                    if (!first) writer.println(",")
                    first = false
                    val obj = JSONObject()
                    for (i in colNames.indices) {
                        val col = colNames[i]
                        val type = cursor.getType(i)
                        when (type) {
                            android.database.Cursor.FIELD_TYPE_NULL -> obj.put(col, JSONObject.NULL)
                            android.database.Cursor.FIELD_TYPE_INTEGER -> obj.put(col, cursor.getLong(i))
                            android.database.Cursor.FIELD_TYPE_FLOAT -> obj.put(col, cursor.getDouble(i))
                            else -> obj.put(col, cursor.getString(i))
                        }
                    }
                    writer.print(obj.toString(2))
                }
            }
            writer.println("\n]")
            writer.flush()
        } finally {
            // Keep writer open or let caller close outputStream
            writer.flush()
        }
    }
}
