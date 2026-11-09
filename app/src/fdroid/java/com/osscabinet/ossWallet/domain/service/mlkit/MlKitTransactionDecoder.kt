package com.osscabinet.ossWallet.domain.service.mlkit

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.dto.TransactionPrefillDTO
import javax.inject.Inject
import javax.inject.Singleton

/**
 * `fdroid` flavor implementation: no Google ML Kit and no Play Services dependency, so the amount,
 * currency and payee all come from [TransactionTextHeuristics], and the date defaults to now.
 * Mirrors the `full` flavor's public API exactly (same package, class name, constructor and
 * `decodeText` signature) so callers never need flavor-aware branching.
 */
@Singleton
class MlKitTransactionDecoder @Inject constructor() {

    suspend fun decodeText(
        rawText: String,
        accounts: List<Account>,
        categories: List<Category>
    ): TransactionPrefillDTO = TransactionTextHeuristics.buildPrefill(rawText, accounts, categories)
}
