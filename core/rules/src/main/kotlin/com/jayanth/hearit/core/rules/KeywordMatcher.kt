package com.jayanth.hearit.core.rules

import com.jayanth.hearit.core.common.model.KeywordMatchMode
import java.util.Locale

/**
 * Case-insensitive keyword/phrase matching shared by the keyword filter (ADR-006 step 7)
 * and the custom sensitive-keyword privacy rule (step 4).
 *
 * All matching happens on `Locale.ROOT`-folded copies of both sides, so results do not
 * depend on the device language or the sender's casing.
 *
 * Whole-word matching is implemented with an explicit `Char.isLetterOrDigit` boundary
 * check instead of a regex `\b`, because `\b` is defined over ASCII word characters and
 * would mis-handle keywords in scripts such as Devanagari — which matters for this app's
 * audience (master prompt §2).
 */
object KeywordMatcher {

    /** True when any non-blank keyword in [keywords] matches [text] under [mode]. Blank entries are ignored. */
    fun matchesAny(text: String, keywords: Collection<String>, mode: KeywordMatchMode): Boolean =
        keywords.any { matches(text, it, mode) }

    /**
     * Checks if a notification matches keyword or sender filter rules.
     * Rules starting with `sender:` check [sender]; all other rules check [messageText].
     */
    fun matchesNotification(
        messageText: String,
        sender: String?,
        keywords: Collection<String>,
        mode: KeywordMatchMode,
    ): Boolean {
        return keywords.any { keyword ->
            val trimmed = keyword.trim()
            if (trimmed.startsWith("sender:", ignoreCase = true)) {
                val senderNeedle = trimmed.substring(7).trim()
                val haystackSender = sender.orEmpty()
                if (senderNeedle.isEmpty()) false
                else matches(haystackSender, senderNeedle, mode)
            } else {
                matches(messageText, keyword, mode)
            }
        }
    }

    /** True when [keyword] matches [text] under [mode]. A blank keyword never matches. */
    fun matches(text: String, keyword: String, mode: KeywordMatchMode): Boolean {
        val trimmed = keyword.trim()
        if (trimmed.isEmpty()) return false

        val (effectiveNeedle, effectiveMode, isPattern) = when {
            trimmed.startsWith("sender:", ignoreCase = true) ->
                Triple(trimmed.substring(7).trim().lowercase(Locale.ROOT), mode, false)
            trimmed.startsWith("word:", ignoreCase = true) ->
                Triple(trimmed.substring(5).trim().lowercase(Locale.ROOT), KeywordMatchMode.WHOLE_WORD, false)
            trimmed.startsWith("phrase:", ignoreCase = true) ->
                Triple(trimmed.substring(7).trim().lowercase(Locale.ROOT), KeywordMatchMode.PHRASE, false)
            trimmed.startsWith("exact:", ignoreCase = true) ->
                Triple(trimmed.substring(6).trim().lowercase(Locale.ROOT), KeywordMatchMode.EXACT, false)
            trimmed.startsWith("pattern:", ignoreCase = true) ->
                Triple(trimmed.substring(8).trim().lowercase(Locale.ROOT), KeywordMatchMode.PHRASE, true)
            trimmed.contains("$") || trimmed.contains("*") ->
                Triple(trimmed.lowercase(Locale.ROOT), mode, true)
            else ->
                Triple(trimmed.lowercase(Locale.ROOT), mode, false)
        }

        if (effectiveNeedle.isEmpty()) return false
        val haystack = text.lowercase(Locale.ROOT)

        if (isPattern) {
            return matchesPattern(haystack, effectiveNeedle)
        }

        return when (effectiveMode) {
            KeywordMatchMode.PHRASE -> haystack.contains(effectiveNeedle)
            KeywordMatchMode.WHOLE_WORD -> containsWholeWord(haystack, effectiveNeedle)
            KeywordMatchMode.EXACT -> haystack == effectiveNeedle
        }
    }

    private fun matchesPattern(haystack: String, pattern: String): Boolean {
        return try {
            val parts = mutableListOf<String>()
            val tokenRegex = Regex("""(\$\d+|\*|\$[a-zA-Z]+)""")
            var lastIndex = 0
            for (match in tokenRegex.findAll(pattern)) {
                if (match.range.first > lastIndex) {
                    parts.add(Regex.escape(pattern.substring(lastIndex, match.range.first)))
                }
                val token = match.value
                when {
                    token == "*" -> parts.add(".*?")
                    token.startsWith("$") -> parts.add("""(?:\d+|\S+)""")
                }
                lastIndex = match.range.last + 1
            }
            if (lastIndex < pattern.length) {
                parts.add(Regex.escape(pattern.substring(lastIndex)))
            }
            val compiled = Regex(parts.joinToString(""), RegexOption.IGNORE_CASE)
            compiled.containsMatchIn(haystack)
        } catch (_: Exception) {
            haystack.contains(pattern)
        }
    }

    private fun containsWholeWord(haystack: String, needle: String): Boolean {
        var index = haystack.indexOf(needle)
        while (index >= 0) {
            val startsAtBoundary = index == 0 || !haystack[index - 1].isLetterOrDigit()
            val endIndex = index + needle.length
            val endsAtBoundary = endIndex >= haystack.length || !haystack[endIndex].isLetterOrDigit()
            if (startsAtBoundary && endsAtBoundary) return true
            index = haystack.indexOf(needle, index + 1)
        }
        return false
    }
}
