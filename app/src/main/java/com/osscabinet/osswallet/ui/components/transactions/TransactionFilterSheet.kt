package com.osscabinet.ossWallet.ui.components.transactions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.data.local.entity.CategoryEntity
import com.osscabinet.ossWallet.domain.model.TransactionFilter

@Composable
fun TransactionFilterSheet(
    filter: TransactionFilter,
    accounts: List<Account>,
    categories: List<CategoryEntity>,
    onSetDateRange: (Long?, Long?) -> Unit,
    onUpdateFilter: (TransactionFilter) -> Unit,
    onToggleExpand: () -> Unit,
    onClose: () -> Unit,
    isExpanded: Boolean,
    backgroundColor: Color = Color(0xFF1E1E1E)
) {
    Column(
        modifier = Modifier.fillMaxHeight(0.9f)
    ) {
        AeonPeriodSelector(
            filter = filter,
            onSetDateRange = onSetDateRange,
            onClick = onToggleExpand,
            isExpanded = isExpanded,
            showArcTab = true,
            backgroundColor = backgroundColor
        )

        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

        FilterSheetContent(
            accounts = accounts,
            allCategories = categories,
            currentFilter = filter,
            onApplyFilter = onUpdateFilter,
            onClose = onClose
        )
    }
}
