package com.osscabinet.ossWallet.domain.service.mlkit

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.AccountStructureType
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.EntryMode
import com.osscabinet.ossWallet.domain.model.dto.TransactionPrefillDTO
import java.math.BigDecimal
import java.util.Currency

/**
 * Turns a pasted bank/payment message into a [TransactionPrefillDTO] with plain regex heuristics.
 *
 * Shared by both build flavors: `full` first asks ML Kit for the amount, currency and date and hands
 * whatever it found to [buildPrefill]; `fdroid` has no ML Kit and passes nothing. Anything missing is
 * filled in here, so both flavors produce the same shape of result.
 */
internal object TransactionTextHeuristics {

    /** What an ML Kit pass managed to pull out of the text. All null when ML Kit found nothing. */
    data class Extracted(
        val amount: BigDecimal? = null,
        val currency: String? = null,
        val timestamp: Long? = null
    )

    fun buildPrefill(
        rawText: String,
        accounts: List<Account>,
        categories: List<Category>,
        extracted: Extracted = Extracted()
    ): TransactionPrefillDTO {
        val fallbackCurrency = accounts.firstOrNull()?.currencyCode ?: DEFAULT_CURRENCY
        if (rawText.isBlank()) {
            return TransactionPrefillDTO(
                amount = "0.00",
                categoryId = Category.SYSTEM_UNKNOWN,
                entryMode = EntryMode.EXPENSE,
                currency = fallbackCurrency
            )
        }

        val amount = extracted.amount ?: parseAmount(rawText)
        val detectedCurrency = extracted.currency ?: parseCurrency("", rawText)
        val payee = extractPayee(rawText)
        val category = matchCategory(payee, rawText, categories)
        val account = matchAccount(rawText, detectedCurrency, accounts)

        return TransactionPrefillDTO(
            amount = amount?.abs()?.toPlainString() ?: "0.00",
            categoryId = category?.id ?: Category.SYSTEM_UNKNOWN,
            entryMode = classifyIntent(rawText),
            accountId = account?.id ?: accounts.firstOrNull()?.id,
            currency = detectedCurrency ?: account?.currencyCode ?: fallbackCurrency,
            payee = payee,
            note = rawText.trim(),
            timestamp = extracted.timestamp ?: System.currentTimeMillis()
        )
    }

    /** The currency named in [spanText] or, failing that, anywhere in [rawText]. */
    fun parseCurrency(spanText: String, rawText: String): String? {
        val searchScope = "$spanText $rawText".uppercase()

        // 1. Direct ISO 4217 3-letter code
        val code = Regex("""\b([A-Z]{3})\b""").find(searchScope)?.groupValues?.get(1)
        if (code != null && isValidIsoCurrency(code)) return code

        // 2. Symbol and abbreviation mappings
        return CURRENCY_MARKERS.firstOrNull { (markers, _) -> markers.any { searchScope.contains(it) } }?.second
    }

    // Currency.getInstance() accepts some non-ISO words (e.g. "TXN"), so check the real ISO list.
    private fun isValidIsoCurrency(code: String): Boolean =
        Currency.getAvailableCurrencies().any { it.currencyCode == code }

    fun parseAmount(rawText: String): BigDecimal? {
        val number = AMOUNT_REGEX.find(rawText)?.groupValues?.get(1) ?: return null

        val sanitized = when {
            number.contains(",") && number.contains(".") ->
                // Whichever separator comes last is the decimal point.
                if (number.lastIndexOf(",") > number.lastIndexOf(".")) number.replace(".", "").replace(",", ".")
                else number.replace(",", "")
            number.contains(",") ->
                // "12,50" is a decimal comma; "1,250" is a thousands separator.
                if (number.substringAfterLast(",").length == 2) number.replace(",", ".") else number.replace(",", "")
            else -> number
        }
        return sanitized.toBigDecimalOrNull()
    }

    private fun classifyIntent(rawText: String): EntryMode {
        // "credit card" names a payment method, not an inflow.
        val text = rawText.lowercase().replace("credit card", "card")
        return when {
            INCOME_KEYWORDS.any { text.contains(it) } -> EntryMode.INCOME
            TRANSFER_KEYWORDS.any { text.contains(it) } -> EntryMode.TRANSFER
            else -> EntryMode.EXPENSE
        }
    }

    private fun extractPayee(rawText: String): String {
        for (pattern in PAYEE_PATTERNS) {
            val match = pattern.find(rawText) ?: continue
            val candidate = match.groupValues[1].trim()
                .split(PAYEE_TERMINATOR)[0]
                .trim()
                .removeSuffix(".")
            if (candidate.length in 2..35) return candidate
        }
        return ""
    }

    private fun matchCategory(payee: String, rawText: String, categories: List<Category>): Category? {
        val searchScope = "$payee $rawText".lowercase()
        return categories.firstOrNull { it.id != Category.SYSTEM_UNKNOWN && searchScope.contains(it.name.lowercase()) }
    }

    /**
     * Picks the target Account or Credit Card with a score:
     * - +50 for matching card / account last 4 digits
     * - +30 for a bank or provider name from the account title appearing in the text
     * - +20 for a credit card mention matching a credit account
     * - +10 for matching currency
     * Falls back to the first account when nothing scores.
     */
    private fun matchAccount(rawText: String, currency: String?, accounts: List<Account>): Account? {
        if (accounts.isEmpty()) return null
        val text = rawText.lowercase()
        val mentionedDigits = CARD_DIGITS_REGEX.findAll(rawText).map { it.groupValues[1] }.toSet()
        val mentionsCard = CARD_WORDS.any { text.contains(it) }

        fun score(account: Account): Int {
            var score = 0
            if (account.id.length >= 4 && account.id.takeLast(4) in mentionedDigits) score += 50
            val nameTokens = account.name.lowercase().split(Regex("""[\s_\-]+""")).filter { it.length >= 3 }
            if (nameTokens.any { text.contains(it) }) score += 30
            if (mentionsCard && account.structureType == AccountStructureType.CREDIT) score += 20
            if (currency != null && account.currencyCode.equals(currency, ignoreCase = true)) score += 10
            return score
        }

        val best = accounts.maxByOrNull(::score)
        return if (best != null && score(best) > 0) best else accounts.first()
    }

    private const val DEFAULT_CURRENCY = "USD"

    private val AMOUNT_REGEX = Regex(
        """(?:Rs\.?|INR|₹|\$|€|£|¥|USD|EUR|GBP|AED|SAR|CHF|AUD|CAD)?\s*""" +
            """([0-9]{1,3}(?:[.,][0-9]{3})+(?:[.,][0-9]{1,2})?|[0-9]+(?:[.,][0-9]{1,2})?)"""
    )

    private val CARD_DIGITS_REGEX = Regex(
        """(?:card|a/c|acc|ending|account|no\.?|num|#|\*|x+)\s*[:#-]?\s*\*?([0-9]{4})\b""",
        RegexOption.IGNORE_CASE
    )

    private val CARD_WORDS = listOf("credit card", "visa", "mastercard", "amex", "card")

    private val INCOME_KEYWORDS = listOf(
        "credit", "credited", "received", "deposit", "deposited", "added",
        "salary", "refund", "refunded", "inflow", "inward", "earned",
        "gutgeschrieben", "eingang", "crédité", "reçu", "acreditado", "recibido", "accredito"
    )

    private val TRANSFER_KEYWORDS = listOf(
        "transfer", "transferred", "moved", "self transfer",
        "überweisung", "virement", "transferencia"
    )

    private val PAYEE_PATTERNS = listOf(
        // "at <merchant>" is the strongest payee signal, so it wins over "for <item>" etc.
        Regex("""\bat\s+([A-Za-z0-9\s&.\-]{2,35})""", RegexOption.IGNORE_CASE),
        Regex(
            """(?:at|to|from|for|vpa|info[:\s]+|merchant[:\s]+|vendor[:\s]+)\s+([A-Za-z0-9\s&.\-]{2,35})""",
            RegexOption.IGNORE_CASE
        ),
        Regex("""paid\s+([A-Za-z0-9\s&.\-]{2,35})""", RegexOption.IGNORE_CASE)
    )

    // Words that end the payee name: "<merchant> on 12 Jan", "<merchant> ref 123", ...
    private val PAYEE_TERMINATOR = Regex(
        """\s+(on|at|ref|txn|a/c|card|ending|bal|balance|with|via|was|is|not|go|to|support|bank|using)\b""",
        RegexOption.IGNORE_CASE
    )

    // Checked in order; the first entry with any marker in the text wins.
    private val CURRENCY_MARKERS: List<Pair<List<String>, String>> = listOf(
        listOf("₹", "INR", "RS.", "RS ") to "INR",
        listOf("$", "USD") to "USD",
        listOf("€", "EUR") to "EUR",
        listOf("£", "GBP") to "GBP",
        listOf("AED") to "AED",
        listOf("SAR") to "SAR",
        listOf("CHF") to "CHF",
        listOf("AUD") to "AUD",
        listOf("CAD") to "CAD",
        listOf("¥", "JPY", "CNY") to "JPY",
        listOf("BRL", "R$") to "BRL",
        listOf("SEK") to "SEK",
        listOf("DKK") to "DKK",
        listOf("NOK") to "NOK",
        listOf("SGD") to "SGD",
        listOf("HKD") to "HKD",
        listOf("NZD") to "NZD",
        listOf("MXN") to "MXN",
        listOf("KRW", "₩") to "KRW",
        listOf("THB", "฿") to "THB",
        listOf("IDR") to "IDR",
        listOf("MYR") to "MYR",
        listOf("PHP", "₱") to "PHP",
        listOf("TRY", "₺") to "TRY",
        listOf("ZAR") to "ZAR",
        listOf("EGP") to "EGP"
    )
}
