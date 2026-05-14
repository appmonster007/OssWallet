package com.osscabinet.ossWallet.domain.model

import kotlinx.serialization.Serializable

/**
 * Represents the method used for a financial transaction.
 *
 * @property displayName Human-readable name for UI display
 * @property iconName Name of the icon to use (maps to AppIconMap in UI layer)
 * @property colorHex Hex color value for the payment method (optional, defaults to primary)
 */
@Serializable
enum class PaymentMethod(
    val displayName: String,
    val iconName: String,
    val colorHex: String? = null
) {
    CASH("Cash", "Payments", "#4CAF50"),
    CREDIT_CARD("Credit Card", "CreditCard", "#2196F3"),
    DEBIT_CARD("Debit Card", "CreditCard", "#03A9F4"),
    BANK_TRANSFER("Bank Transfer", "AccountBalance", "#607D8B"),
    DIGITAL_WALLET("Digital Wallet", "Wallet", "#FF9800"),
    MOBILE_PAYMENT("Mobile Payment", "Contactless", "#9C27B0"),
    CHECK("Check", "Description", "#795548"),
    CRYPTOCURRENCY("Cryptocurrency", "CurrencyBitcoin", "#FFC107"),
    OTHER("Other", "MoreHoriz", "#9E9E9E");

    companion object {
        fun fromString(value: String): PaymentMethod {
            return entries.find { it.displayName.equals(value, ignoreCase = true) } ?: OTHER
        }
    }
}
