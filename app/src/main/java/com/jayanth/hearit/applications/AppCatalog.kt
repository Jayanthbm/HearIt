package com.jayanth.hearit.applications

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The app catalog for the Applications feature (P13).
 *
 * A seam so the ViewModel performs no framework calls and the list stays testable.
 */
interface ApplicationCatalog {

    /** Installed apps the user can enable, sorted by label. Suspends: reads packages off-thread. */
    suspend fun installedApps(): List<AppEntry>
}

/**
 * [ApplicationCatalog] over launcher activities.
 *
 * **Deliberately launcher-based.** Android 11+ package visibility limits what a normal app
 * can enumerate, and `QUERY_ALL_PACKAGES` is a Play-restricted permission this project
 * does not request (no Play Store requirement in v1, but widening visibility would still
 * be a privacy regression for a notification reader). The manifest's `<queries>` element
 * (P11) makes launcher apps visible, which covers the apps a user would expect to enable.
 * Apps without a launcher entry stay reachable once enabled — the ViewModel merges enabled
 * packages back into the list (ADR-025), so nothing enabled can become unmanageable.
 */
class AndroidApplicationCatalog(private val context: Context) : ApplicationCatalog {

    override suspend fun installedApps(): List<AppEntry> = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        packageManager.queryIntentActivities(launcherIntent, 0)
            .asSequence()
            .mapNotNull { it.activityInfo?.applicationInfo }
            .filter { it.packageName != context.packageName }
            .distinctBy { it.packageName }
            .map { info ->
                val isSystem = (info.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
                AppEntry(
                    packageName = info.packageName,
                    label = runCatching { packageManager.getApplicationLabel(info).toString() }
                        .getOrDefault(info.packageName),
                    isSystemApp = isSystem,
                )
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }
}

/** Loads one app icon for the list, off the main thread. */
interface AppIconLoader {

    /** The app's icon as an [ImageBitmap], or null when it cannot be loaded. */
    suspend fun load(packageName: String): ImageBitmap?
}

/**
 * [AppIconLoader] over the package manager.
 *
 * Missing icons are not an error: the row simply renders without one, so a broken package
 * can never fail the list. Sizing avoids holding full-size launcher bitmaps in memory.
 */
class AndroidAppIconLoader(private val context: Context) : AppIconLoader {

    override suspend fun load(packageName: String): ImageBitmap? = withContext(Dispatchers.IO) {
        runCatching {
            context.packageManager.getApplicationIcon(packageName)
                .toBitmap(ICON_SIZE_PX, ICON_SIZE_PX, Bitmap.Config.ARGB_8888)
                .asImageBitmap()
        }.getOrNull()
    }

    private companion object {
        /** 48 dp at 2× density: crisp at the list's icon size without a large allocation. */
        const val ICON_SIZE_PX = 96
    }
}
