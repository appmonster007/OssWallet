package com.osscabinet.ossWallet.ui.components.entry

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Tag
import com.osscabinet.ossWallet.domain.model.Template
import java.math.BigDecimal

/**
 * Snapshot of external data loaded by [BaseEntryViewModel] for all entry forms.
 * Combine of: profile core data, shortlisted currencies, and exchange rates.
 */
data class EntryFormSnapshot(
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val allTags: List<Tag> = emptyList(),
    val templates: List<Template> = emptyList(),
    val allPayees: List<String> = emptyList(),
    val allNotes: List<String> = emptyList(),
    val shortlistedCurrencies: List<String> = emptyList(),
    val fullProviderCurrencies: List<String> = emptyList(),
    val exchangeRates: Map<String, BigDecimal> = emptyMap(),
)
