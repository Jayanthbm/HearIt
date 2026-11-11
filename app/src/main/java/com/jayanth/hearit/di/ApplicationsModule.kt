package com.jayanth.hearit.di

import android.content.Context
import com.jayanth.hearit.applications.AndroidAppIconLoader
import com.jayanth.hearit.applications.AndroidApplicationCatalog
import com.jayanth.hearit.applications.AppIconLoader
import com.jayanth.hearit.applications.ApplicationCatalog
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Applications-feature bindings (P13, ADR-025).
 *
 * Both are stateless adapters over the package manager, so a process singleton is enough;
 * scoping them also keeps the catalog's deduplication cheap.
 */
@Module
@InstallIn(SingletonComponent::class)
object ApplicationsModule {

    @Provides
    @Singleton
    fun applicationCatalog(@ApplicationContext context: Context): ApplicationCatalog =
        AndroidApplicationCatalog(context)

    @Provides
    @Singleton
    fun appIconLoader(@ApplicationContext context: Context): AppIconLoader =
        AndroidAppIconLoader(context)
}
