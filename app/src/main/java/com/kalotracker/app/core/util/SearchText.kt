package com.kalotracker.app.core.util

import java.text.Normalizer
import java.util.Locale

/** Literal local search: whitespace/punctuation tolerant, with every query word required. */
internal object SearchText {
    private val words = Regex("[\\p{L}\\p{N}\\p{M}]+")

    fun normalized(text: String): String = words.findAll(
        Normalizer.normalize(text, Normalizer.Form.NFKC).lowercase(Locale.ROOT)
    ).joinToString(" ") { it.value }

    fun matches(query: String, fields: Iterable<String>): Boolean {
        if (query.isBlank()) return true
        val terms = normalized(query).split(' ').filter { it.isNotEmpty() }
        if (terms.isEmpty()) return false
        val text = fields.joinToString(" ") { normalized(it) }
        return terms.all { text.contains(it) }
    }
}
