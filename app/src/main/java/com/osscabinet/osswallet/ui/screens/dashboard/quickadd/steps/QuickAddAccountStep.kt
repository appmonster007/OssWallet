package com.osscabinet.ossWallet.ui.screens.dashboard.quickadd.steps

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.AccountStructureType
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.ui.components.getAccountIconByName
import com.osscabinet.ossWallet.ui.components.GlassSurface
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.scaledDp
import com.osscabinet.ossWallet.ui.theme.GlassVariant
import com.osscabinet.ossWallet.ui.theme.GlassTokens
import com.osscabinet.ossWallet.ui.theme.copyStyle
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState

@Composable
fun QuickAddAccountStep(
    accounts: List<Account>,
    selectedAccountId: String?,
    transactionType: TransactionType,
    onAccountTapped: (String) -> Unit,
    onToggleType: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var structureFilter by remember { mutableStateOf<AccountStructureType?>(null) }
    val uiScale = AeonTheme.uiScale

    val structureTypes = remember(accounts) {
        accounts.map { it.structureType }.distinct().sortedBy { it.displayName }
    }

    val filtered = remember(accounts, searchQuery, structureFilter) {
        accounts.filter { acc ->
            (structureFilter == null || acc.structureType == structureFilter) &&
                    (searchQuery.isBlank() || acc.name.contains(searchQuery, ignoreCase = true))
        }
    }

    val typeColor = if (transactionType == TransactionType.EXPENSE)
        Color(android.graphics.Color.parseColor(TransactionType.EXPENSE.colorHex))
    else
        Color(android.graphics.Color.parseColor(TransactionType.INCOME.colorHex))

    val toggleVPad = scaledDp(10f, uiScale, min = 7f)
    val toggleIconSize = scaledDp(18f, uiScale, min = 14f)
    val columnSpacing = scaledDp(10f, uiScale, min = 7f)
    val tileSize = scaledDp(72f, uiScale, min = 56f)
    val tileCorner = scaledDp(20f, uiScale, min = 14f)
    val tileIconSize = scaledDp(28f, uiScale, min = 20f)
    val gridMinSize = scaledDp(88f, uiScale, min = 68f)
    val gridSpacing = scaledDp(8f, uiScale, min = 6f)
    val itemSpacing = scaledDp(6f, uiScale, min = 4f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(columnSpacing)
    ) {
        // Transaction type toggle
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            glassVariant = GlassVariant.Item,
            hazeStyle = GlassTokens.itemStyle().copyStyle(bgColor = typeColor, alpha = 0.12f),
            fallbackColor = typeColor.copy(alpha = 0.12f)
        ) {
            Surface(
                onClick = onToggleType,
                shape = RoundedCornerShape(12.dp),
                color = Color.Transparent,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = toggleVPad),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (transactionType == TransactionType.EXPENSE)
                            Icons.Rounded.TrendingDown else Icons.Rounded.TrendingUp,
                        contentDescription = null,
                        tint = typeColor,
                        modifier = Modifier.size(toggleIconSize)
                    )
                    Text(
                        text = transactionType.displayName,
                        style = AppTextStyles.formSwitchLabel,
                        color = typeColor
                    )
                    Text(
                        text = "· tap to switch",
                        style = AppTextStyles.chartAxisLabel,
                        color = typeColor.copy(alpha = 0.5f)
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

        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search accounts...", style = AppTextStyles.formFieldHint) },
            leadingIcon = {
                Icon(
                    Icons.Rounded.Search,
                    contentDescription = null,
                    tint = AppColors.contentSecondary
                )
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppColors.primary,
                unfocusedBorderColor = AppColors.divider.copy(alpha = 0.3f),
                focusedContainerColor = AppColors.recessedSurface,
                unfocusedContainerColor = AppColors.recessedSurface
            ),
            singleLine = true
        )

        // Account grid
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = gridMinSize),
            verticalArrangement = Arrangement.spacedBy(gridSpacing),
            horizontalArrangement = Arrangement.spacedBy(gridSpacing),
            contentPadding = PaddingValues(bottom = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(filtered, key = { it.id }) { account ->
                AccountGridItem(
                    account = account,
                    isSelected = account.id == selectedAccountId,
                    transactionType = transactionType,
                    onClick = { onAccountTapped(account.id) },
                    tileSize = tileSize,
                    tileCorner = tileCorner,
                    tileIconSize = tileIconSize,
                    itemSpacing = itemSpacing
                )
            }
        }
    }
}

@Composable
private fun AccountGridItem(
    account: Account,
    isSelected: Boolean,
    transactionType: TransactionType,
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
        if (transactionType == TransactionType.EXPENSE)
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
