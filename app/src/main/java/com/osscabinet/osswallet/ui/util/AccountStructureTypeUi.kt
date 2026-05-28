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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.osscabinet.ossWallet.domain.model.AccountStructureType

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

val AccountStructureType.color: Color
    get() = when (this) {
        AccountStructureType.BANK -> Color(0xFF2196F3)
        AccountStructureType.SAVINGS -> Color(0xFF4CAF50)
        AccountStructureType.CASH -> Color(0xFF8BC34A)
        AccountStructureType.DIGITAL_WALLET -> Color(0xFF00BCD4)
        AccountStructureType.CREDIT -> Color(0xFFF44336)
        AccountStructureType.INVESTMENT -> Color(0xFFFF9800)
        AccountStructureType.CRYPTO -> Color(0xFFFFC107)
        AccountStructureType.OTHER -> Color(0xFF9E9E9E)
    }
