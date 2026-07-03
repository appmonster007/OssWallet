package com.osscabinet.ossWallet.ui.components.entry

import com.osscabinet.ossWallet.domain.model.AmountEditingSide

/**
 * Common visibility state for entry forms.
 */
data class FormSheetState(
    val showAccount: Boolean = false,
    val showTargetAccount: Boolean = false,
    val showCategory: Boolean = false,
    val showPaymentMethod: Boolean = false,
    val showStatus: Boolean = false,
    val showTag: Boolean = false,
    val showCurrency: Boolean = false,
    val showTargetCurrency: Boolean = false,
    val showExchangeRate: Boolean = false,
    val showDate: Boolean = false,
    val showWarrantyDate: Boolean = false,
    val showLocation: Boolean = false,
    val showLinkedTransaction: Boolean = false,
    val isNumpadVisible: Boolean = false,
    val activeAmountSide: AmountEditingSide = AmountEditingSide.SOURCE
)
