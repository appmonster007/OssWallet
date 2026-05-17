package com.osscabinet.ossWallet.ui.components.transactions

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.AccountStructureType
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Filter
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import java.math.BigDecimal
import com.osscabinet.ossWallet.ui.theme.AppColors

@Composable
fun TransactionFilterSheet(
    filter: TransactionFilter,
    accounts: List<Account>,
    categories: List<Category>,
    savedFilters: List<Filter> = emptyList(),
    onFilterEvent: (FilterEvent) -> Unit,
    onToggleExpand: () -> Unit,
    onClose: () -> Unit,
    backgroundColor: Color = AppColors.primaryContainer,
    showAccountFilter: Boolean = true,
    accountNames: Map<String, String> = emptyMap(),
) {
    Box(
        modifier = Modifier.wrapContentHeight()
    ) {
        FilterSheetContent(
            filter = filter,
            accounts = accounts,
            allCategories = categories,
            savedFilters = savedFilters,
            onFilterEvent = onFilterEvent,
            onToggleExpand = onToggleExpand,
            onClose = onClose,
            backgroundColor = backgroundColor,
            showAccountFilter = showAccountFilter,
            accountNames = accountNames,
        )
    }
}

@Preview(showBackground = false)
@Composable
fun TransactionFilterSheetPreview() {
    var theme by remember { mutableStateOf("Light") }
    OssWalletTheme(theme = theme) {
        Surface(color = AppColors.screenBackground) {
            Box(modifier = Modifier.fillMaxSize()) {
                TransactionFilterSheet(
                    filter = TransactionFilter(),
                    accounts = listOf(
                        Account(
                            id = "1",
                            name = "Cash",
                            currencyCode = "USD",
                            structureType = AccountStructureType.CASH,
                            colorHex = "#FF0000",
                            icon = "wallet"
                        ),
                        Account(
                            id = "2",
                            name = "Bank",
                            currencyCode = "USD",
                            structureType = AccountStructureType.CHECKING,
                            colorHex = "#00FF00",
                            icon = "account_balance"
                        )
                    ),
                    categories = listOf(
                        Category(
                            id = 1,
                            name = "Food",
                            iconName = "restaurant",
                            colorHex = 0xFFFF0000.toInt(),
                            isDefault = true,
                            parentId = null
                        ),
                        Category(
                            id = 2,
                            name = "Transport",
                            iconName = "directions_bus",
                            colorHex = 0xFF00FF00.toInt(),
                            isDefault = true,
                            parentId = null
                        )
                    ),
                    onFilterEvent = {},
                    onToggleExpand = {},
                    onClose = {}
                )

                // Theme Switcher
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(onClick = { theme = "Light" }) { Text("Light") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { theme = "Dark" }) { Text("Dark") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { theme = "Amoled" }) { Text("Amoled") }
                }
            }
        }
    }
}
