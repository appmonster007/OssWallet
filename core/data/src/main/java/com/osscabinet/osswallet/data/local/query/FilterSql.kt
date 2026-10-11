package com.osscabinet.ossWallet.data.local.query

import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.util.toSmartRegex
import kotlinx.serialization.json.Json

/**
 * Translates the non-structural parts of a [TransactionFilter] to SQL, in one place, so that the
 * aggregation queries behind cards and the repository's list query agree with each other and with
 * the in-memory `TransactionFilterService.matches`, which is the reference behaviour.
 *
 * Only bindable types go into `args` (String / Long / Int); anything else makes SQLite binding throw.
 */
internal object FilterSql {

    private val json = Json

    /** Regex note/payee criteria can't be expressed in SQL; they are resolved to ids in Kotlin. */
    fun hasRegexCriteria(filter: TransactionFilter): Boolean =
        (filter.isNoteRegex && !filter.queryNote.isNullOrBlank()) ||
                (filter.isPayeeRegex && !filter.queryPayee.isNullOrBlank())

    /** Same test the in-memory filter applies to the regex criteria (invalid regex matches nothing). */
    fun matchesRegexCriteria(filter: TransactionFilter, note: String, payee: String): Boolean {
        val queryNote = filter.queryNote
        if (filter.isNoteRegex && !queryNote.isNullOrBlank()) {
            val ok = runCatching { queryNote.toSmartRegex().containsMatchIn(note) }.getOrDefault(false)
            if (!ok) return false
        }
        val queryPayee = filter.queryPayee
        if (filter.isPayeeRegex && !queryPayee.isNullOrBlank()) {
            val ok = runCatching { queryPayee.toSmartRegex().containsMatchIn(payee) }.getOrDefault(false)
            if (!ok) return false
        }
        return true
    }

    /**
     * Appends category, type, currency, amount, payment method, warranty, tag, text, status and
     * linked-transaction conditions. Timeline and account scoping stay with the callers.
     */
    fun appendCriteria(where: StringBuilder, args: MutableList<Any>, filter: TransactionFilter) {
        if (filter.categoryIds.isNotEmpty()) {
            where.append(" AND categoryId IN (${placeholders(filter.categoryIds.size)})")
            args.addAll(filter.categoryIds)
        }
        if (filter.transactionTypes.isNotEmpty()) {
            where.append(" AND transactionType IN (${placeholders(filter.transactionTypes.size)})")
            args.addAll(filter.transactionTypes.map { it.value })
        }
        if (filter.currencies.isNotEmpty()) {
            where.append(" AND currency IN (${placeholders(filter.currencies.size)})")
            args.addAll(filter.currencies)
        }

        // Amounts are stored as TEXT; compare numerically on the absolute value (matches the
        // in-memory filter). The bound value is a String because BigDecimal can't be bound.
        filter.minAmount?.let {
            where.append(" AND ABS(CAST(amount AS REAL)) >= CAST(? AS REAL)")
            args.add(it.toPlainString())
        }
        filter.maxAmount?.let {
            where.append(" AND ABS(CAST(amount AS REAL)) <= CAST(? AS REAL)")
            args.add(it.toPlainString())
        }

        if (filter.paymentMethods.isNotEmpty()) {
            where.append(" AND paymentMethod IN (${placeholders(filter.paymentMethods.size)})")
            args.addAll(filter.paymentMethods.map { it.name })
        }

        when (filter.hasWarranty) {
            true -> where.append(" AND warrantyExpiration IS NOT NULL")
            false -> where.append(" AND warrantyExpiration IS NULL")
            null -> Unit
        }
        filter.warrantyExpirationStart?.let {
            where.append(" AND warrantyExpiration >= ?")
            args.add(it)
        }
        filter.warrantyExpirationEnd?.let {
            where.append(" AND warrantyExpiration <= ?")
            args.add(it)
        }

        // Tags are a JSON array of strings in one column, so an exact tag is its quoted JSON form.
        // instr (not LIKE) keeps it case-sensitive like the in-memory filter and needs no escaping.
        if (filter.tags.isNotEmpty()) {
            val joiner = if (filter.matchAllTags) " AND " else " OR "
            where.append(" AND (")
            where.append(filter.tags.joinToString(joiner) { "instr(tags, ?) > 0" })
            where.append(")")
            filter.tags.forEach { args.add(json.encodeToString(it)) }
        }

        val queryNote = filter.queryNote
        if (!queryNote.isNullOrBlank() && !filter.isNoteRegex) {
            where.append(" AND note LIKE ? ESCAPE '\\'")
            args.add(likeContains(queryNote))
        }
        val queryPayee = filter.queryPayee
        if (!queryPayee.isNullOrBlank() && !filter.isPayeeRegex) {
            where.append(" AND payee LIKE ? ESCAPE '\\'")
            args.add(likeContains(queryPayee))
        }

        if (filter.statuses.isNotEmpty()) {
            where.append(" AND status IN (${placeholders(filter.statuses.size)})")
            args.addAll(filter.statuses.map { it.name })
        }

        // Ids are Longs, so they are inlined: no bind-variable limit, no injection surface.
        if (filter.linkedTransactionIds.isNotEmpty()) {
            where.append(" AND id IN (${filter.linkedTransactionIds.joinToString(",")})")
        }
    }

    fun appendIdRestriction(where: StringBuilder, ids: Set<Long>?) {
        if (ids == null) return
        if (ids.isEmpty()) where.append(" AND 0") else where.append(" AND id IN (${ids.joinToString(",")})")
    }

    private fun placeholders(count: Int) = List(count) { "?" }.joinToString(",")

    private fun likeContains(text: String): String =
        "%" + text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%"
}
