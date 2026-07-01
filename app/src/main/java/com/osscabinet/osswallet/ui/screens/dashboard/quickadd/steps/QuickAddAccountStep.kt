package com.osscabinet.ossWallet.ui.screens.dashboard.quickadd.steps

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.AccountStructureType
import com.osscabinet.ossWallet.domain.model.EntryMode
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.ui.components.AeonSearchField
import com.osscabinet.ossWallet.ui.components.aeonHairlineBorder
import com.osscabinet.ossWallet.ui.components.getAccountIconByName
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme

/**
 * Step 2 — "Atelier Account". Editorial Expense/Income toggle, scrollable
 * structure-type filter chips, a glass panel of account tiles, and a search field.
 * Selection takes on the semantic transaction-type color (red for expense,
 * green for income).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickAddAccountStep(
    accounts: List<Account>,
    structureTypes: List<AccountStructureType>,
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

    val filtered = remember(accounts, searchQuery, structureFilter) {
        accounts.filter { acc ->
            (structureFilter == null || acc.structureType == structureFilter) &&
                    (searchQuery.isBlank() || acc.name.contains(searchQuery, ignoreCase = true))
        }
    }

    val typeColor = remember(entryMode) {
        val hex = when (entryMode) {
            EntryMode.EXPENSE -> TransactionType.EXPENSE.colorHex
            EntryMode.INCOME -> TransactionType.INCOME.colorHex
            EntryMode.TRANSFER -> TransactionType.TRANSFER_OUTFLOW.colorHex
        }
        Color(android.graphics.Color.parseColor(hex))
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val numColumns = if (maxWidth > 400.dp) 5 else 4

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Editorial structure-type filter rail
            StructureChipRail(
                structures = structureTypes,
                selected = structureFilter,
                onSelect = { structureFilter = it },
                activeColor = typeColor,
            )

            AtelierPanel(
                overline = if (searchQuery.isNotBlank()) "Results" else "Accounts",
                modifier = Modifier.weight(1f, fill = false),
            ) {
                val fallbackIconColor = AppColors.contentSecondary
                if (filtered.isEmpty()) {
                    EmptyState("No accounts found")
                } else {
                    AtelierGrid(numColumns, Modifier.weight(1f, fill = false)) {
                        items(filtered, key = { it.id }) { account ->
                            val accountColor = remember(account.colorHex, fallbackIconColor) {
                                runCatching {
                                    Color(android.graphics.Color.parseColor(account.colorHex))
                                }.getOrDefault(fallbackIconColor)
                            }
                            AtelierTile(
                                label = account.name,
                                icon = getAccountIconByName(account.icon),
                                iconColor = accountColor,
                                isSelected = account.id == selectedAccountId,
                                selectionColor = typeColor,
                                selectionContentColor = Color.White,
                                onTap = { onAccountTapped(account.id) }
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AtelierTypeToggle(
                    entryMode = entryMode,
                    onClick = onToggleType,
                    activeColor = typeColor
                )

                AeonSearchField(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = "Search accounts",
                    modifier = Modifier.weight(1f),
                    focusRequester = focusRequester
                )
            }
        }
    }
}

@Composable
private fun StructureChipRail(
    structures: List<AccountStructureType>,
    selected: AccountStructureType?,
    onSelect: (AccountStructureType?) -> Unit,
    activeColor: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        AtelierChip(
            label = "ALL",
            isSelected = selected == null,
            activeColor = activeColor,
            onClick = { onSelect(null) }
        )
        structures.forEach { type ->
            AtelierChip(
                label = type.displayName.uppercase(),
                isSelected = selected == type,
                activeColor = activeColor,
                onClick = { onSelect(if (selected == type) null else type) }
            )
        }
    }
}

/**
 * Visually 28dp tall, but the tappable region extends to a 48dp minimum so the
 * chip meets Android's touch-target guidance. The pill is rendered inside a
 * larger transparent clickable Box rather than fighting Material's
 * `LocalMinimumInteractiveComponentSize`.
 */
@Composable
private fun AtelierChip(
    label: String,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    val bg = if (isSelected) activeColor.copy(alpha = 0.14f) else AppColors.recessedSurface
    val contentColor = if (isSelected) activeColor else AppColors.contentSecondary
    val borderModifier = if (isSelected)
        Modifier.aeonHairlineBorder(shape, accent = activeColor)
    else Modifier.aeonHairlineBorder(shape)

    Box(
        modifier = Modifier
            .defaultMinSize(minHeight = 28.dp)
            .clip(shape)
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = label
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .height(28.dp)
                .clip(shape)
                .background(bg)
                .then(borderModifier)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = AppTextStyles.sectionOverline,
                color = contentColor
            )
        }
    }
}

/**
 * Icon-only transaction-type toggle (Expense ↔ Income). Renders the
 * current mode as a high-contrast directional arrow (TrendingDown for
 * Expense, TrendingUp for Income) at 22dp inside a 48dp tappable surface.
 * The surface picks up a faint [activeColor] tint so the mode is legible
 * at a glance; the icon is the redundant signal. Full word goes to the
 * semantics layer for screen-reader users.
 */
@Composable
private fun AtelierTypeToggle(
    entryMode: EntryMode,
    onClick: () -> Unit,
    activeColor: Color,
) {
    val shape = RoundedCornerShape(12.dp)
    val isIncome = entryMode == EntryMode.INCOME
    val icon = if (isIncome) Icons.AutoMirrored.Rounded.TrendingUp
    else Icons.AutoMirrored.Rounded.TrendingDown
    val a11yLabel = if (isIncome) "Income, tap to switch to Expense"
    else "Expense, tap to switch to Income"

    Surface(
        onClick = onClick,
        shape = shape,
        color = activeColor.copy(alpha = 0.10f),
        modifier = Modifier
            .size(width = 56.dp, height = 48.dp)
            .aeonHairlineBorder(shape, accent = activeColor)
            .semantics {
                role = Role.Button
                contentDescription = a11yLabel
            }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = activeColor,
                modifier = Modifier.size(22.dp)
            )
        }
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
                structureTypes = AccountStructureType.entries,
                selectedAccountId = "1",
                entryMode = EntryMode.EXPENSE,
                onAccountTapped = {},
                onToggleType = {}
            )
        }
    }
}
