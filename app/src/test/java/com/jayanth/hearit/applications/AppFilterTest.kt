package com.jayanth.hearit.applications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Search, merge and selection tests (P13). Written, **not executed** — cloud-agent
 * restriction.
 */
class AppFilterTest {

    private val whatsapp = AppEntry("com.whatsapp", "WhatsApp")
    private val messages = AppEntry("com.google.android.apps.messaging", "Messages")
    private val bank = AppEntry("com.example.bank", "Example Bank")
    private val all = listOf(bank, messages, whatsapp)

    @Test
    fun filter_blankQuery_returnsEveryApp() {
        assertEquals(all, AppFilter.filter(all, ""))
        assertEquals(all, AppFilter.filter(all, "   "))
    }

    @Test
    fun filter_matchesLabelIgnoringCase() {
        assertEquals(listOf(whatsapp), AppFilter.filter(all, "whatsapp"))
        assertEquals(listOf(whatsapp), AppFilter.filter(all, "WHATS"))
    }

    @Test
    fun filter_matchesPackageName() {
        assertEquals(listOf(bank), AppFilter.filter(all, "example.bank"))
    }

    @Test
    fun filter_trimsTheQuery() {
        assertEquals(listOf(messages), AppFilter.filter(all, "  mess  "))
    }

    @Test
    fun filter_withoutMatch_returnsEmpty() {
        assertTrue(AppFilter.filter(all, "zzz").isEmpty())
    }

    @Test
    fun merge_keepsCatalogEntriesAndTheirLabels() {
        val merged = AppCatalogMerge.merge(catalog = listOf(whatsapp, bank), enabledPackages = setOf("com.whatsapp"))

        assertEquals(listOf(bank, whatsapp), merged)
        assertEquals("WhatsApp", merged.first { it.packageName == "com.whatsapp" }.label)
    }

    @Test
    fun merge_addsEnabledPackageTheCatalogCannotSee() {
        val merged = AppCatalogMerge.merge(
            catalog = listOf(whatsapp),
            enabledPackages = setOf("com.hidden.app"),
        )

        val hidden = merged.firstOrNull { it.packageName == "com.hidden.app" }
        assertTrue(hidden != null)
        assertEquals("com.hidden.app", hidden?.label)
    }

    @Test
    fun merge_sortsByLabelIgnoringCase() {
        val merged = AppCatalogMerge.merge(
            catalog = listOf(AppEntry("b", "zebra"), AppEntry("a", "Apple"), AppEntry("c", "mango")),
            enabledPackages = emptySet(),
        )

        assertEquals(listOf("Apple", "mango", "zebra"), merged.map { it.label })
    }

    @Test
    fun merge_ignoresBlankEnabledPackage() {
        val merged = AppCatalogMerge.merge(catalog = listOf(whatsapp), enabledPackages = setOf(""))

        assertEquals(listOf(whatsapp), merged)
    }

    @Test
    fun enable_addsTargetsAndKeepsOtherEnabledApps() {
        val result = AppSelection.enable(
            enabledPackages = setOf("com.existing"),
            targets = listOf("com.whatsapp", "com.example.bank"),
        )

        assertEquals(setOf("com.existing", "com.whatsapp", "com.example.bank"), result)
    }

    @Test
    fun disable_removesOnlyTargets() {
        val result = AppSelection.disable(
            enabledPackages = setOf("com.existing", "com.whatsapp"),
            targets = listOf("com.whatsapp"),
        )

        assertEquals(setOf("com.existing"), result)
    }
}
