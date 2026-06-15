package com.osscabinet.ossWallet.ui.screens.dashboard.quickadd.steps

import com.osscabinet.ossWallet.domain.model.EntryMode
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.AccountStructureType
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.ui.components.getAccountIconByName
import com.osscabinet.ossWallet.ui.components.GlassSurface
import com.osscabinet.ossWallet.ui.components.AeonSearchField
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.GlassVariant
import com.osscabinet.ossWallet.ui.theme.GlassTokens
import com.osscabinet.ossWallet.ui.theme.copyStyle
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.rememberScrollState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickAddAccountStep(
    accounts: List<Account>,
    selectedAccountId: String?,
    entryMode: EntryMode,
    isStepActive: Boolean = false,
    onAccountTapped: (String) -> Unit,
    onToggleType: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var structureFilter by remember { mutableStateOf<AccountStructureType?>(null) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isStepActive) {
        if (isStepActive) focusRequester.requestFocus()
    }

    val structureTypes = remember(accounts) {
        accounts.map { it.structureType }.distinct().sortedBy { it.displayName }
    }

    val filtered = remember(accounts, searchQuery, structureFilter) {
        accounts.filter { acc ->
            (structureFilter == null || acc.structureType == structureFilter) &&
                    (searchQuery.isBlank() || acc.name.contains(searchQuery, ignoreCase = true))
        }
    }

    val typeColor = if (entryMode == EntryMode.EXPENSE)
        Color(android.graphics.Color.parseColor(TransactionType.EXPENSE.colorHex))
    else
        Color(android.graphics.Color.parseColor(TransactionType.INCOME.colorHex))

    val toggleIconSize = 16.dp
    val columnSpacing  = 7.dp
    val chipHeight     = 32.dp
    val tileSize       = 56.dp
    val tileCorner     = 14.dp
    val tileIconSize   = 20.dp
    val gridSpacing    = 6.dp
    val itemSpacing    = 4.dp

    // 2-row max; 1 row while the keyboard is open so the sheet fits above it
    val imeVisible = WindowInsets.isImeVisible
    val rowHeight    = tileSize + itemSpacing + 20.dp
    val maxGridHeight = if (imeVisible) rowHeight else (rowHeight * 2) + gridSpacing

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val numColumns = if (maxWidth > 400.dp) 5 else 4

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(columnSpacing)
        ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Transaction type toggle
            GlassSurface(
                modifier = Modifier.height(chipHeight),
                shape = RoundedCornerShape(10.dp),
                glassVariant = GlassVariant.Item,
                hazeStyle = GlassTokens.itemStyle().copyStyle(bgColor = typeColor, alpha = 0.12f),
                fallbackColor = typeColor.copy(alpha = 0.12f),
                borderColor = typeColor.copy(alpha = 0.4f),
                borderWidth = 1.dp
            ) {
                Surface(
                    modifier = Modifier.fillMaxHeight(),
                    onClick = onToggleType,
                    shape = RoundedCornerShape(10.dp),
                    color = Color.Transparent,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (entryMode == EntryMode.EXPENSE)
                                Icons.Rounded.TrendingDown else Icons.Rounded.TrendingUp,
                            contentDescription = null,
                            tint = typeColor,
                            modifier = Modifier.size(toggleIconSize)
                        )
                        Text(
                            text = entryMode.displayName,
                            style = AppTextStyles.chartAxisLabel,
                            color = typeColor
                        )
                    }
                }
            }

            // Structure type filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = structureFilter == null,
                    onClick = { structureFilter = null },
                    label = { Text("All", style = AppTextStyles.chartAxisLabel) },
                    shape = RoundedCornerShape(10.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AppColors.primary.copy(alpha = 0.15f),
                        selectedLabelColor = AppColors.primary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = structureFilter == null,
                        selectedBorderColor = AppColors.primary.copy(alpha = 0.4f),
                        borderColor = AppColors.divider.copy(alpha = 0.3f)
                    )
                )
                structureTypes.forEach { type ->
                    FilterChip(
                        selected = structureFilter == type,
                        onClick = { structureFilter = if (structureFilter == type) null else type },
                        label = { Text(type.displayName, style = AppTextStyles.chartAxisLabel) },
                        shape = RoundedCornerShape(10.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AppColors.primary.copy(alpha = 0.15f),
                            selectedLabelColor = AppColors.primary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = structureFilter == type,
                            selectedBorderColor = AppColors.primary.copy(alpha = 0.4f),
                            borderColor = AppColors.divider.copy(alpha = 0.3f)
                        )
                    )
                }
            }
        }

        // Account grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(numColumns),
            verticalArrangement = Arrangement.spacedBy(gridSpacing),
            horizontalArrangement = Arrangement.spacedBy(gridSpacing),
            contentPadding = PaddingValues(bottom = 4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxGridHeight)
        ) {
            items(filtered, key = { it.id }) { account ->
                AccountGridItem(
                    account = account,
                    isSelected = account.id == selectedAccountId,
                    entryMode = entryMode,
                    onClick = { onAccountTapped(account.id) },
                    tileSize = tileSize,
                    tileCorner = tileCorner,
                    tileIconSize = tileIconSize,
                    itemSpacing = itemSpacing
                )
            }
        }

        // Search bar
        AeonSearchField(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            placeholder = "Search accounts...",
            modifier = Modifier.fillMaxWidth(),
            focusRequester = focusRequester
        )
    }
    }
}

@Composable
private fun AccountGridItem(
    account: Account,
    isSelected: Boolean,
    entryMode: EntryMode,
    onClick: () -> Unit,
    tileSize: androidx.compose.ui.unit.Dp = 72.dp,
    tileCorner: androidx.compose.ui.unit.Dp = 20.dp,
    tileIconSize: androidx.compose.ui.unit.Dp = 28.dp,
    itemSpacing: androidx.compose.ui.unit.Dp = 6.dp
) {
    val primaryColor = AppColors.primary
    val accountColor = remember(account.colorHex) {
        runCatching { Color(android.graphics.Color.parseColor(account.colorHex)) }
            .getOrDefault(primaryColor)
    }

    val borderColor = if (isSelected) {
        if (entryMode == EntryMode.EXPENSE)
            Color(android.graphics.Color.parseColor(TransactionType.EXPENSE.colorHex))
        else
            Color(android.graphics.Color.parseColor(TransactionType.INCOME.colorHex))
    } else Color.Transparent

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(itemSpacing)
    ) {
        GlassSurface(
            modifier = Modifier.size(tileSize),
            shape = RoundedCornerShape(tileCorner),
            glassVariant = GlassVariant.Item,
            hazeStyle = GlassTokens.itemStyle().copyStyle(bgColor = accountColor, alpha = 0.15f),
            fallbackColor = accountColor.copy(alpha = 0.15f),
            borderColor = if (isSelected) borderColor else null,
            borderWidth = if (isSelected) 2.dp else 0.dp
        ) {
            Surface(
                onClick = onClick,
                modifier = Modifier.fillMaxSize(),
                color = Color.Transparent,
                shape = RoundedCornerShape(tileCorner)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = getAccountIconByName(account.icon),
                        contentDescription = account.name,
                        tint = accountColor,
                        modifier = Modifier.size(tileIconSize)
                    )
                }
            }
        }
        Text(
            text = account.name,
            style = AppTextStyles.chartAxisLabel,
            color = AppColors.contentPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewQuickAddAccountStep() {
    val mockAccounts = listOf(
        Account("1", "Cash", "USD", AccountStructureType.CASH, "#4CAF50", "Payments"),
        Account("2", "Bank", "USD", AccountStructureType.BANK, "#2196F3", "AccountBalance"),
        Account("3", "Credit Card", "USD", AccountStructureType.CREDIT, "#F44336", "CreditCard")
    )

    OssWalletTheme {
        Box(modifier = Modifier.background(AppColors.background)) {
            QuickAddAccountStep(
                accounts = mockAccounts,
                selectedAccountId = "1",
                entryMode = EntryMode.EXPENSE,
                onAccountTapped = {},
                onToggleType = {}
            )
        }
    }
}
