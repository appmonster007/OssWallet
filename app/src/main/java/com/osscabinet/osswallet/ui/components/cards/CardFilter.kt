package com.osscabinet.ossWallet.ui.components.cards

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.settings.*

/**
 * Centralized filtering logic for card data.
 * Extension functions on CardConfig types to filter accounts and transactions
 * based on card configuration properties (accountId, accountIds).
 * 
 * This centralizes filtering logic and makes it easier to maintain.
 */

/**
 * Filter accounts based on card configuration.
 * - NetWorthCardConfig: filters by filter.accountIds if specified
 * - CurrencyBalancesCardConfig: filters by filter.accountIds if specified
 * - Other configs: returns all accounts (no filtering)
 */
fun CardConfig.filterAccounts(accounts: List<Account>): List<Account> {
    return when (this) {
        is NetWorthCardConfig -> {
            if (filter.accountIds.isNotEmpty()) {
                accounts.filter { it.id in filter.accountIds }
            } else {
                accounts
            }
        }
        is CurrencyBalancesCardConfig -> {
            if (filter.accountIds.isNotEmpty()) {
                accounts.filter { it.id in filter.accountIds }
            } else {
                accounts
            }
        }
        is CashFlowCardConfig -> {
            if (filter.accountIds.isNotEmpty()) {
                accounts.filter { it.id in filter.accountIds }
            } else {
                accounts
            }
        }
        is AccountsCardConfig -> {
            if (filter.accountIds.isNotEmpty()) {
                accounts.filter { it.id in filter.accountIds }
            } else {
                accounts
            }
        }
        else -> accounts
    }
}

/**
 * Filter transactions based on card configuration.
 */
fun CardConfig.filterTransactions(transactions: List<Transaction>): List<Transaction> {
    return when (this) {
        is NetWorthCardConfig -> {
            if (filter.accountIds.isNotEmpty()) {
                transactions.filter { it.accountId in filter.accountIds }
            } else {
                transactions
            }
        }
        is CurrencyBalancesCardConfig -> {
            if (filter.accountIds.isNotEmpty()) {
                transactions.filter { it.accountId in filter.accountIds }
            } else {
                transactions
            }
        }
        is CashFlowCardConfig -> {
            if (filter.accountIds.isNotEmpty()) {
                transactions.filter { it.accountId in filter.accountIds }
            } else {
                transactions
            }
        }
        is AccountsCardConfig -> {
            if (filter.accountIds.isNotEmpty()) {
                transactions.filter { it.accountId in filter.accountIds }
            } else {
                transactions
            }
        }
        else -> transactions
    }
}
