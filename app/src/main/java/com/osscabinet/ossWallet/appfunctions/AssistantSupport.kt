package com.osscabinet.ossWallet.appfunctions

import com.osscabinet.ossWallet.domain.model.EntryMode
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Currency

/** Why a request couldn't be served; mapped to the system's error types at the service boundary. */
class AssistantException(val kind: Kind, message: String) : Exception(message) {
    enum class Kind { INVALID_ARGUMENT, NOT_FOUND, DENIED, DISABLED }

    companion object {
        fun invalid(message: String) = AssistantException(Kind.INVALID_ARGUMENT, message)
        fun notFound(message: String) = AssistantException(Kind.NOT_FOUND, message)
    }
}

/** Input checking and number/text shaping for assistant calls. Pure, so it is tested without a device. */
object AssistantSupport {
    const val MAX_AMOUNT = 1_000_000_000L
    const val MAX_NOTE = 500
    const val MAX_TAGS = 10
    const val MAX_TAG_LENGTH = 40
    const val DEFAULT_LIMIT = 20
    const val MAX_LIMIT = 50

    /** What an entry matched: one name, nothing, or several that fit equally well. */
    sealed interface Match {
        data class Found(val name: String) : Match
        data object None : Match
        data class Ambiguous(val options: List<String>) : Match
    }

    /**
     * Finds [wanted] among [names]: an exact match (ignoring case) wins, then a unique "starts with", then a unique
     * "contains". Several equally good candidates are reported rather than guessed between.
     */
    fun match(names: List<String>, wanted: String): Match {
        val w = wanted.trim()
        if (w.isEmpty()) return Match.None
        names.filter { it.equals(w, ignoreCase = true) }.let { exact ->
            if (exact.size == 1) return Match.Found(exact.single())
            if (exact.size > 1) return Match.Ambiguous(exact.distinct())
        }
        for (test in listOf<(String) -> Boolean>({ it.startsWith(w, true) }, { it.contains(w, true) })) {
            val hits = names.filter(test).distinct()
            if (hits.size == 1) return Match.Found(hits.single())
            if (hits.size > 1) return Match.Ambiguous(hits)
        }
        return Match.None
    }

    /** "expense"/"income" (and a few natural variants) as an entry mode; blank means expense. */
    fun parseType(text: String?): EntryMode = when (text?.trim()?.lowercase()) {
        null, "", "expense", "expenses", "spending", "spend", "payment", "debit" -> EntryMode.EXPENSE
        "income", "earning", "earnings", "salary", "credit", "deposit" -> EntryMode.INCOME
        else -> throw AssistantException.invalid("type must be \"expense\" or \"income\", not \"$text\"")
    }

    /** A positive amount with at most two decimals. Currency symbols and thousands separators are tolerated. */
    fun parseAmount(text: String): BigDecimal {
        val cleaned = text.trim().replace(Regex("[\\s$€£₹¥,]"), "")
        val value = cleaned.toBigDecimalOrNull()
            ?: throw AssistantException.invalid("amount must be a number such as 12.50, not \"$text\"")
        if (value.signum() <= 0) throw AssistantException.invalid("amount must be greater than zero")
        if (value > BigDecimal(MAX_AMOUNT)) throw AssistantException.invalid("amount is too large")
        if (value.stripTrailingZeros().scale() > 2) throw AssistantException.invalid("amount can have at most two decimals")
        return value.setScale(2, RoundingMode.UNNECESSARY)
    }

    /** The moment as epoch millis in [zone]; blank means [now]. Dates far in the past or future are refused as mistakes. */
    fun resolveTime(dateTime: LocalDateTime?, now: Long, zone: ZoneId): Long {
        if (dateTime == null) return now
        val millis = dateTime.atZone(zone).toInstant().toEpochMilli()
        val years = java.time.Duration.ofDays(365)
        if (millis < now - years.multipliedBy(20).toMillis()) throw AssistantException.invalid("that date is more than 20 years ago")
        if (millis > now + years.toMillis()) throw AssistantException.invalid("that date is more than a year ahead")
        return millis
    }

    fun cleanNote(note: String?): String {
        val n = note?.trim().orEmpty()
        if (n.length > MAX_NOTE) throw AssistantException.invalid("note is longer than $MAX_NOTE characters")
        return n
    }

    fun cleanTags(tags: List<String>): List<String> {
        val cleaned = tags.map { it.trim().removePrefix("#").trim() }.filter { it.isNotEmpty() }.distinctBy { it.lowercase() }
        if (cleaned.size > MAX_TAGS) throw AssistantException.invalid("at most $MAX_TAGS tags")
        if (cleaned.any { it.length > MAX_TAG_LENGTH }) throw AssistantException.invalid("a tag is longer than $MAX_TAG_LENGTH characters")
        return cleaned
    }

    fun clampLimit(limit: Int): Int = when {
        limit <= 0 -> DEFAULT_LIMIT
        else -> minOf(limit, MAX_LIMIT)
    }

    /** Plain text with the currency's own number of decimals (JPY has none), never grouped or in scientific form. */
    fun money(amount: BigDecimal, currency: String): String {
        val digits = runCatching { Currency.getInstance(currency).defaultFractionDigits }.getOrDefault(2).coerceAtLeast(0)
        return amount.setScale(digits, RoundingMode.HALF_UP).toPlainString()
    }

    fun percent(part: BigDecimal, whole: BigDecimal): String =
        if (whole.signum() == 0) "0.0"
        else part.multiply(BigDecimal(100)).divide(whole, 1, RoundingMode.HALF_UP).toPlainString()

    fun isoLocal(millis: Long, zone: ZoneId): String =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), zone).withNano(0).toString()

    fun candidates(names: List<String>, limit: Int = 8): String = names.take(limit).joinToString(", ") { "\"$it\"" } +
        if (names.size > limit) ", …" else ""
}
