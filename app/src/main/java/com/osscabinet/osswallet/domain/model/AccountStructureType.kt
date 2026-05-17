package com.osscabinet.ossWallet.domain.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class AccountStructureType(
    val displayName: String,
    val icon: ImageVector,
    val color: Color
) {
    SAVINGS("Savings", Icons.Rounded.Savings, Color(0xFF4CAF50)),
    CREDIT_CARD("Credit Card", Icons.Rounded.CreditCard, Color(0xFFF44336)),
    CHECKING("Checking", Icons.Rounded.AccountBalance, Color(0xFF2196F3)),
    INVESTMENT("Investment", Icons.AutoMirrored.Rounded.TrendingUp, Color(0xFFFF9800)),
    CASH("Cash", Icons.Rounded.Payments, Color(0xFF8BC34A)),
    OTHER("Other", Icons.Rounded.Layers, Color(0xFF9E9E9E));

    companion object {
        fun fromString(value: String): AccountStructureType {
            return values().find { it.displayName.equals(value, ignoreCase = true) } ?: SAVINGS
        }
    }
}
