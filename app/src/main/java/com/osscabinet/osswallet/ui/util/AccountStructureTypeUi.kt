package com.osscabinet.ossWallet.ui.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.osscabinet.ossWallet.domain.model.AccountStructureType

val AccountStructureType.icon: ImageVector
    get() = when (this) {
        AccountStructureType.SAVINGS -> Icons.Rounded.Savings
        AccountStructureType.CREDIT_CARD -> Icons.Rounded.CreditCard
        AccountStructureType.CHECKING -> Icons.Rounded.AccountBalance
        AccountStructureType.INVESTMENT -> Icons.AutoMirrored.Rounded.TrendingUp
        AccountStructureType.CASH -> Icons.Rounded.Payments
        AccountStructureType.OTHER -> Icons.Rounded.Layers
    }

val AccountStructureType.color: Color
    get() = when (this) {
        AccountStructureType.SAVINGS -> Color(0xFF4CAF50)
        AccountStructureType.CREDIT_CARD -> Color(0xFFF44336)
        AccountStructureType.CHECKING -> Color(0xFF2196F3)
        AccountStructureType.INVESTMENT -> Color(0xFFFF9800)
        AccountStructureType.CASH -> Color(0xFF8BC34A)
        AccountStructureType.OTHER -> Color(0xFF9E9E9E)
    }
