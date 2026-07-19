package com.osscabinet.ossWallet.ui.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.CurrencyBitcoin
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.ui.graphics.vector.ImageVector
import com.osscabinet.ossWallet.domain.model.AccountStructureType
import com.osscabinet.ossWallet.ui.theme.AccountStructureColors

val AccountStructureType.icon: ImageVector
    get() = when (this) {
        AccountStructureType.BANK -> Icons.Rounded.AccountBalance
        AccountStructureType.SAVINGS -> Icons.Rounded.Savings
        AccountStructureType.CASH -> Icons.Rounded.Payments
        AccountStructureType.DIGITAL_WALLET -> Icons.Rounded.AccountBalanceWallet
        AccountStructureType.CREDIT -> Icons.Rounded.CreditCard
        AccountStructureType.INVESTMENT -> Icons.AutoMirrored.Rounded.TrendingUp
        AccountStructureType.CRYPTO -> Icons.Rounded.CurrencyBitcoin
        AccountStructureType.OTHER -> Icons.Rounded.Layers
    }

val AccountStructureType.color
    get() = when (this) {
        AccountStructureType.BANK -> AccountStructureColors.bank
        AccountStructureType.SAVINGS -> AccountStructureColors.savings
        AccountStructureType.CASH -> AccountStructureColors.cash
        AccountStructureType.DIGITAL_WALLET -> AccountStructureColors.digitalWallet
        AccountStructureType.CREDIT -> AccountStructureColors.credit
        AccountStructureType.INVESTMENT -> AccountStructureColors.investment
        AccountStructureType.CRYPTO -> AccountStructureColors.crypto
        AccountStructureType.OTHER -> AccountStructureColors.other
    }
