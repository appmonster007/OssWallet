package com.osscabinet.ossWallet.domain.service.search

import java.math.BigDecimal
import java.math.RoundingMode

/** An amount the user typed: one value (49.99) or a bound/range (>100, <50, 20-50). Signs are ignored. */
data class AmountSpec(val min: BigDecimal?, val max: BigDecimal?) {
    fun matches(amount: BigDecimal): Boolean {
        val a = amount.abs()
        return (min == null || a >= min) && (max == null || a <= max)
    }

    companion object {
        /** One exact value, compared to the cent so 49.99 doesn't miss 49.990000. */
        fun exact(value: BigDecimal): AmountSpec {
            val v = value.setScale(2, RoundingMode.HALF_UP)
            return AmountSpec(v, v)
        }
    }
}

/**
 * What a search box entry means.
 *
 * - words: free text ("laptop")
 * - tags: `#holiday`
 * - amount: `49.99`, `>100`, `<50`, `20-50`
 * - [numberOnly]: the whole entry was one plain number, so it may be an amount or part of a name ("iPhone 15").
 */
data class ParsedSearch(
    val words: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val amount: AmountSpec? = null,
    val numberOnly: Boolean = false
) {
    val isEmpty: Boolean get() = words.isEmpty() && tags.isEmpty() && amount == null
}

object SearchQueryParser {
    private const val CURRENCY = "[$€£₹¥]?"
    private val operator = Regex("^(>=|<=|>|<)$CURRENCY(\\d[\\d,]*(?:\\.\\d+)?)$")
    private val range = Regex("^$CURRENCY(\\d[\\d,]*(?:\\.\\d+)?)-$CURRENCY(\\d[\\d,]*(?:\\.\\d+)?)$")
    private val number = Regex("^$CURRENCY(\\d[\\d,]*(?:\\.\\d+)?)$")

    private fun isYear(token: String) = token.length == 4 && token.toIntOrNull() in 1990..2100

    fun parse(input: String): ParsedSearch {
        val tokens = input.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val words = mutableListOf<String>()
        val tags = mutableListOf<String>()
        var min: BigDecimal? = null
        var max: BigDecimal? = null
        var exact: BigDecimal? = null
        var operatorUsed = false

        for (token in tokens) {
            when {
                token.length > 1 && token.startsWith("#") -> tags += token.drop(1).lowercase()
                operator.matches(token) -> {
                    val (op, value) = operator.matchEntire(token)!!.destructured
                    val v = value.replace(",", "").toBigDecimalOrNull() ?: continue
                    operatorUsed = true
                    if (op.startsWith(">")) min = v else max = v
                }
                range.matches(token) -> {
                    val (a, b) = range.matchEntire(token)!!.destructured
                    val lo = a.replace(",", "").toBigDecimalOrNull()
                    val hi = b.replace(",", "").toBigDecimalOrNull()
                    if (lo != null && hi != null) {
                        operatorUsed = true
                        min = lo.min(hi); max = lo.max(hi)
                    }
                }
                // "rent 2026" means the year, not a payment of exactly 2026; alone, "2026" could be either.
                isYear(token) && tokens.size > 1 -> words += token
                number.matches(token) && exact == null -> exact = number.matchEntire(token)!!.groupValues[1].replace(",", "").toBigDecimalOrNull()
                else -> words += token.lowercase()
            }
        }

        // A lone number might be part of a name as much as an amount, so it stays searchable as text too.
        val numberOnly = exact != null && words.isEmpty() && tags.isEmpty() && !operatorUsed
        if (numberOnly) words += tokens.first().trimStart('$', '€', '£', '₹', '¥')

        val amount = when {
            exact != null -> AmountSpec.exact(exact)
            min != null || max != null -> AmountSpec(min, max)
            else -> null
        }
        return ParsedSearch(words, tags, amount, numberOnly)
    }
}
