package com.jayanth.hearit.core.common.model

/** How keywords are matched against notification text (master prompt §6, "Categories and keywords"). */
enum class KeywordMatchMode {
    /**
     * The keyword must appear as a whole word/phrase delimited by non-letter/digit
     * characters (or the ends of the text), so "ash" does not match "cash". Boundary
     * detection is script-aware via `Char.isLetterOrDigit`, so it also works for
     * non-Latin scripts such as Devanagari, where regex `\b` would not.
     */
    WHOLE_WORD,

    /** The keyword may appear anywhere inside the text, including inside other words. */
    PHRASE,
}

/**
 * Custom keyword filtering (master prompt §6).
 *
 * Filtering is **disabled by default**. Matching is always case-insensitive
 * (`Locale.ROOT` folding, no locale-sensitive `toLowerCase()`).
 *
 * @property enabled Master switch; off by default.
 * @property keywords User keywords/phrases; blank entries are ignored at match time.
 * @property mode Whole-word versus substring/phrase matching.
 */
data class KeywordFilterSettings(
    val enabled: Boolean = false,
    val keywords: List<String> = emptyList(),
    val mode: KeywordMatchMode = KeywordMatchMode.WHOLE_WORD,
)
