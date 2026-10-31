package com.osscabinet.ossWallet.domain.service.mlkit

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.AccountStructureType
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.EntryMode
import com.osscabinet.ossWallet.domain.model.dto.TransactionPrefillDTO
import java.math.BigDecimal
import java.util.Currency
import javax.inject.Inject
import javax.inject.Singleton

/**
 * `fdroid` flavor implementation: universal transaction decoder using only the regex/heuristic
 * fallback logic — no Google ML Kit, no Play Services dependency. Mirrors the `full` flavor
 * implementation's public API exactly (same package, same class name, same constructor, same
 * `decodeText` signature) so callers never need flavor-aware branching.
 *
 * ML Kit in the `full` flavor is only used for two things here, both of which already have (or
 * fall through to) a heuristic equivalent:
 *  - MoneyEntity extraction — the `full` flavor already falls back to [parseUniversalAmount]
 *    whenever ML Kit finds nothing, so this flavor just goes straight to that fallback.
 *  - DateTimeEntity extraction — the `full` flavor has no fallback for this and falls through to
 *    `System.currentTimeMillis()` when ML Kit finds nothing anyway, so this flavor does the same
 *    unconditionally.
 */
@Singleton
class MlKitTransactionDecoder @Inject constructor() {

    suspend fun decodeText(
        rawText: String,
        accounts: List<Account>,
        categories: List<Category>
    ): TransactionPrefillDTO {
        if (rawText.isBlank()) {
            return TransactionPrefillDTO(
                amount = "0.00",
                categoryId = Category.SYSTEM_UNKNOWN,
                entryMode = EntryMode.EXPENSE,
                currency = accounts.firstOrNull()?.currencyCode ?: "USD"
            )
        }

        // 1. Universal amount parsing (regex fallback — no ML Kit MoneyEntity extraction)
        val extractedAmount: BigDecimal? = parseUniversalAmount(rawText)
        val extractedCurrency: String? = parseCurrencyFromSpan("", rawText)

        // 2. No ML Kit DateTimeEntity extraction available; default timestamp below.

        // 3. Financial intent classification
        val entryMode = classifyIntent(rawText)

        // 4. Payee extraction
        val payeeName = extractPayee(rawText)

        // 5. Category matching
        val matchedCategory = matchCategory(payeeName, rawText, categories)

        // 6. Intelligent Account & Credit Card matching
        val matchedAccount = matchAccount(rawText, extractedCurrency, accounts)

        val finalAmount = extractedAmount?.abs()?.toPlainString() ?: "0.00"
        val currencyCode = extractedCurrency
            ?: matchedAccount?.currencyCode
            ?: accounts.firstOrNull()?.currencyCode
            ?: "USD"

        return TransactionPrefillDTO(
            amount = finalAmount,
            categoryId = matchedCategory?.id ?: Category.SYSTEM_UNKNOWN,
            entryMode = entryMode,
            accountId = matchedAccount?.id ?: accounts.firstOrNull()?.id,
            currency = currencyCode,
            payee = payeeName,
            note = rawText.trim(),
            timestamp = System.currentTimeMillis()
        )
    }

    private fun parseCurrencyFromSpan(spanText: String, rawText: String): String? {
        val searchScope = "$spanText $rawText".uppercase()

        // 1. Direct ISO 4217 3-letter code match
        val codeMatch = Regex("""\b([A-Z]{3})\b""").find(searchScope)
        if (codeMatch != null) {
            val candidate = codeMatch.groupValues[1]
            if (isValidIsoCurrency(candidate)) {
                return candidate
            }
        }

        // 2. Symbol mappings
        return when {
            searchScope.contains("₹") || searchScope.contains("INR") || searchScope.contains("RS.") || searchScope.contains("RS ") -> "INR"
            searchScope.contains("$") || searchScope.contains("USD") -> "USD"
            searchScope.contains("€") || searchScope.contains("EUR") -> "EUR"
            searchScope.contains("£") || searchScope.contains("GBP") -> "GBP"
            searchScope.contains("AED") -> "AED"
            searchScope.contains("SAR") -> "SAR"
            searchScope.contains("CHF") -> "CHF"
            searchScope.contains("AUD") -> "AUD"
            searchScope.contains("CAD") -> "CAD"
            searchScope.contains("¥") || searchScope.contains("JPY") || searchScope.contains("CNY") -> "JPY"
            searchScope.contains("BRL") || searchScope.contains("R$") -> "BRL"
            searchScope.contains("SEK") -> "SEK"
            searchScope.contains("DKK") -> "DKK"
            searchScope.contains("NOK") -> "NOK"
            searchScope.contains("SGD") -> "SGD"
            searchScope.contains("HKD") -> "HKD"
            searchScope.contains("NZD") -> "NZD"
            searchScope.contains("MXN") -> "MXN"
            searchScope.contains("KRW") || searchScope.contains("₩") -> "KRW"
            searchScope.contains("THB") || searchScope.contains("฿") -> "THB"
            searchScope.contains("IDR") -> "IDR"
            searchScope.contains("MYR") -> "MYR"
            searchScope.contains("PHP") || searchScope.contains("₱") -> "PHP"
            searchScope.contains("TRY") || searchScope.contains("₺") -> "TRY"
            searchScope.contains("ZAR") -> "ZAR"
            searchScope.contains("EGP") -> "EGP"
            else -> null
        }
    }

    private fun isValidIsoCurrency(code: String): Boolean {
        return try {
            Currency.getInstance(code) != null
        } catch (_: Exception) {
            false
        }
    }

    private fun parseUniversalAmount(rawText: String): BigDecimal? {
        val regex = Regex("""(?:Rs\.?|INR|₹|\$|€|£|¥|USD|EUR|GBP|AED|SAR|CHF|AUD|CAD)?\s*([0-9]{1,3}(?:[.,][0-9]{3})*(?:[.,][0-9]{1,2})?|[0-9]+(?:[.,][0-9]{1,2})?)""")
        val match = regex.find(rawText) ?: return null
        val numStr = match.groupValues[1]

        val sanitized = if (numStr.contains(",") && numStr.contains(".")) {
            if (numStr.lastIndexOf(",") > numStr.lastIndexOf(".")) {
                numStr.replace(".", "").replace(",", ".")
            } else {
                numStr.replace(",", "")
            }
        } else if (numStr.contains(",")) {
            val parts = numStr.split(",")
            if (parts.last().length == 2) {
                numStr.replace(",", ".")
            } else {
                numStr.replace(",", "")
            }
        } else {
            numStr
        }

        return sanitized.toBigDecimalOrNull()
    }

    private fun classifyIntent(rawText: String): EntryMode {
        val lowerText = rawText.lowercase()

        val incomeKeywords = listOf(
            "credit", "credited", "received", "deposit", "deposited", "added",
            "salary", "refund", "refunded", "inflow", "inward", "earned",
            "gutgeschrieben", "eingang", "crédité", "reçu", "acreditado", "recibido", "accredito"
        )

        val transferKeywords = listOf(
            "transfer", "transferred", "moved", "self transfer",
            "überweisung", "virement", "transferencia"
        )

        return when {
            incomeKeywords.any { lowerText.contains(it) } -> EntryMode.INCOME
            transferKeywords.any { lowerText.contains(it) } -> EntryMode.TRANSFER
            else -> EntryMode.EXPENSE
        }
    }

    private fun extractPayee(rawText: String): String {
        val patterns = listOf(
            Regex("""(?:at|to|from|for|vpa|info[:\s]+|merchant[:\s]+|vendor[:\s]+)\s+([A-Za-z0-9\s&.\-]{2,35})""", RegexOption.IGNORE_CASE),
            Regex("""paid\s+([A-Za-z0-9\s&.\-]{2,35})""", RegexOption.IGNORE_CASE)
        )

        for (pattern in patterns) {
            val match = pattern.find(rawText)
            if (match != null) {
                val candidate = match.groupValues[1].trim()
                    .split(Regex("""\s+(on|at|ref|txn|a/c|card|ending|bal|balance|with|via|was|is|not|go|to|support|bank)\b""", RegexOption.IGNORE_CASE))[0]
                    .trim()
                    .removeSuffix(".")
                if (candidate.length in 2..35) {
                    return candidate
                }
            }
        }
        return ""
    }

    private fun matchCategory(payee: String, rawText: String, categories: List<Category>): Category? {
        val searchScope = "$payee $rawText".lowercase()
        return categories.firstOrNull { cat ->
            cat.id != Category.SYSTEM_UNKNOWN && searchScope.contains(cat.name.lowercase())
        }
    }

    /**
     * Intelligently selects the target Account or Credit Card using a scoring system:
     * - +50 points for matching card / account last 4 digits
     * - +30 points for matching bank or provider name in account title
     * - +20 points for credit card structure type alignment
     * - +10 points for currency match
     */
    private fun matchAccount(rawText: String, extractedCurrency: String?, accounts: List<Account>): Account? {
        if (accounts.isEmpty()) return null
        val lowerText = rawText.lowercase()

        val cardDigitsRegex = Regex("""(?:card|a/c|acc|ending|account|no\.?|num|#|\*|x+)\s*[:#-]?\s*\*?([0-9]{4})\b""", RegexOption.IGNORE_CASE)
        val digitMatches = cardDigitsRegex.findAll(rawText).map { it.groupValues[1] }.toSet()

        val isCreditCardMentioned = lowerText.contains("credit card") ||
                lowerText.contains("visa") ||
                lowerText.contains("mastercard") ||
                lowerText.contains("amex") ||
                lowerText.contains("card")

        var bestAccount: Account? = null
        var maxScore = -1

        for (acc in accounts) {
            var score = 0

            // A) Last 4 digits match (+50 points)
            if (acc.id.length >= 4 && digitMatches.contains(acc.id.takeLast(4))) {
                score += 50
            }

            // B) Bank or Provider name match in Account Title (+30 points)
            val accNameLower = acc.name.lowercase()
            if (accNameLower.isNotBlank()) {
                val nameTokens = accNameLower.split(Regex("""[\s_\-]+""")).filter { it.length >= 3 }
                for (token in nameTokens) {
                    if (lowerText.contains(token)) {
                        score += 30
                        break
                    }
                }
            }

            // C) Credit Card Structure Alignment (+20 points)
            if (isCreditCardMentioned && acc.structureType == AccountStructureType.CREDIT) {
                score += 20
            }

            // D) Currency Alignment (+10 points)
            if (extractedCurrency != null && acc.currencyCode.equals(extractedCurrency, ignoreCase = true)) {
                score += 10
            }

            if (score > maxScore) {
                maxScore = score
                bestAccount = acc
            }
        }

        return if (maxScore > 0) bestAccount else accounts.firstOrNull()
    }
}
