package com.osscabinet.ossWallet.ui.components.entry

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.AccountStructureType
import com.osscabinet.ossWallet.ui.components.AeonSelectMode
import com.osscabinet.ossWallet.ui.components.getAccountIconByName
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.ThemePreviews

@Composable
fun AeonAccountSelection(
    accounts: List<Account>,
    selectedAccountIds: Set<String>,
    onSelectionChange: (Set<String>) -> Unit,
    onDismiss: () -> Unit,
    mode: AeonSelectMode = AeonSelectMode.SINGLE,
    title: String = "Select Account",
    description: String? = null
) {
    AeonSelectionSheet(
        title = title,
        onDismiss = onDismiss,
        items = accounts,
        selectedIds = selectedAccountIds,
        onSelectionChange = onSelectionChange,
        idMapper = { it.id },
        labelMapper = { it.name },
        mode = mode,
        iconMapper = { getAccountIconByName(it.icon) },
        colorMapper = { Color(android.graphics.Color.parseColor(it.colorHex)) },
        detailMapper = { it.currencyCode.ifBlank { "External" } },
        searchPlaceholder = "Search accounts...",
        headerContent = if (description != null) {
            {
                Text(
                    text = description,
                    style = AppTextStyles.formFieldNote,
                    color = AppColors.contentSecondary,
                    modifier = Modifier
                )
            }
        } else null
    )
}

@ThemePreviews
@Composable
fun AeonAccountSelectionPreview() {
    OssWalletTheme {
        AeonAccountSelection(
            accounts = listOf(
                Account.OUTSIDE_WALLET,
                Account(
                    id = "1",
                    name = "Savings",
                    colorHex = "#4CAF50",
                    currencyCode = "USD",
                    structureType = AccountStructureType.SAVINGS,
                    icon = "AccountBalance"
                ),
                Account(
                    id = "2",
                    name = "Checking",
                    colorHex = "#2196F3",
                    currencyCode = "USD",
                    structureType = AccountStructureType.BANK,
                    icon = "AccountBalance"
                )
            ),
            selectedAccountIds = setOf("1"),
            onSelectionChange = {},
            onDismiss = {}
        )
    }
}
