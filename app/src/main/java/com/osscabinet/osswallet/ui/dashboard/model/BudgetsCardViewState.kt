package com.osscabinet.ossWallet.ui.dashboard.model

/**
 * Lean ViewState for BudgetsCard
 * Pre-computed budget progress items with color determination
 */
data class BudgetsCardViewState(
    override val cardId: String,
    override val isLoading: Boolean = false,
    val budgetItems: List<BudgetDisplayItem>,
    val showProgressLabels: Boolean,
    val error: String? = null
) : CardViewState {
    
    companion object {
        fun loading(cardId: String) = BudgetsCardViewState(
            cardId = cardId,
            isLoading = true,
            budgetItems = emptyList(),
            showProgressLabels = true
        )
        
        fun error(cardId: String, message: String) = BudgetsCardViewState(
            cardId = cardId,
            isLoading = false,
            budgetItems = emptyList(),
            showProgressLabels = true,
            error = message
        )
    }
}

/**
 * Pre-computed display item for a single budget
 * All formatting and color determination done in ViewModel/Domain layer
 */
data class BudgetDisplayItem(
    val category: String,
    val formattedSpent: String,
    val formattedLimit: String,
    val progress: Float, // 0.0 to 1.0
    val isOverBudget: Boolean,
    val showLabels: Boolean
)

/**
 * Intents for BudgetsCard
 */
sealed class BudgetsCardIntent : CardIntent {
    data class UpdateShowProgressLabels(
        override val cardId: String,
        val show: Boolean
    ) : BudgetsCardIntent()
}
