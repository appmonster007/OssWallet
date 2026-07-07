package com.osscabinet.ossWallet.ui.components.cards

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.osscabinet.ossWallet.domain.card.CardFilterRules
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Filter
import com.osscabinet.ossWallet.domain.model.Tag
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.ui.components.transactions.FilterEvent
import kotlinx.coroutines.flow.Flow

/**
 * Carries everything [TransactionFilterPill] needs to render when it is lifted
 * out of a Dialog and rendered at the screen level by a host composable.
 *
 * All callback lambdas should be stable (captured via rememberUpdatedState at the
 * call site) so that card recompositions don't recreate the request object needlessly.
 */
data class FilterPillRequest(
    val filter: TransactionFilter,
    val accounts: List<Account>,
    val categories: List<Category>,
    val allTags: List<Tag>,
    val savedFilters: List<Filter>,
    val onSearchTransactions: (String) -> Flow<List<Transaction>>,
    val onFilterEvent: (FilterEvent) -> Unit,
    val onSaveFilterClick: (TransactionFilter) -> Unit,
    val onDismiss: () -> Unit,
    val filterRules: CardFilterRules,
    val baseFilter: TransactionFilter,
)

/**
 * Screen-level controller that [AeonExpressiveCard] writes to instead of opening a Dialog,
 * and that the host screen (e.g. [DashboardScreen]) reads to render [TransactionFilterPill]
 * directly in the main composition tree — enabling real Haze glass blur.
 *
 * Screens that don't provide this local (Analytics, etc.) cause [AeonExpressiveCard] to
 * fall back to the existing Dialog behaviour automatically.
 */
@Stable
class FilterPillController {
    var activeRequest: FilterPillRequest? by mutableStateOf(null)
        private set

    fun show(request: FilterPillRequest) {
        activeRequest = request
    }

    fun dismiss() {
        activeRequest = null
    }
}

val LocalFilterPillController = staticCompositionLocalOf<FilterPillController?> { null }
