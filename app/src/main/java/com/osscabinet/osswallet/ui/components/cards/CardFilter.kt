package com.osscabinet.ossWallet.ui.components.cards

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.settings.*

/**
 * Centralized filtering logic for card data.
 * Extension functions on CardConfig types to filter accounts and transactions
 * based on card configuration properties (accountId, selectedAccountIds).
 * 
 * This centralizes filtering logic and makes it easier to maintain.
 */

/**
 * Filter accounts based on card configuration.
 * - NetWorthCardConfig: filters by selectedAccountIds if specified
 * - CurrencyBalancesCardConfig: filters by accountId if specified
 * - Other configs: returns all accounts (no filtering)
 */
fun CardConfig.filterAccounts(accounts: List<Account>): List<Account> {
    return when (this) {
        is NetWorthCardConfig -> {
            if (selectedAccountIds != null && selectedAccountIds.isNotEmpty()) {
                accounts.filter { it.id in selectedAccountIds }
            } else {
                accounts
            }
        }
        is CurrencyBalancesCardConfig -> {
            if (accountId != null) {
                accounts.filter { it.id == accountId }
            } else {
                accounts
            }
        }
        else -> accounts
    }
}

/**
 * Filter transactions based on card configuration.
 * - NetWorthCardConfig: filters by selectedAccountIds if specified
 * - CurrencyBalancesCardConfig: filters by accountId if specified
 * - CashFlowCardConfig: filters by accountId if specified
 * - Other configs: returns all transactions (no filtering)
 */
fun CardConfig.filterTransactions(transactions: List<Transaction>): List<Transaction> {
    return when (this) {
        is NetWorthCardConfig -> {
            if (selectedAccountIds != null && selectedAccountIds.isNotEmpty()) {
                transactions.filter { it.accountId in selectedAccountIds }
            } else {
                transactions
            }
        }
        is CurrencyBalancesCardConfig -> {
            if (accountId != null) {
                transactions.filter { it.accountId == accountId }
            } else {
                transactions
            }
        }
        is CashFlowCardConfig -> {
            if (accountId != null) {
                transactions.filter { it.accountId == accountId }
            } else {
                transactions
            }
        }
        else -> transactions
    }
}
