package com.osscabinet.ossWallet.ui.screens.import_csv.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.osscabinet.ossWallet.domain.model.AccountStructureType
import com.osscabinet.ossWallet.domain.model.import_csv.EntityMapping
import com.osscabinet.ossWallet.ui.components.AeonFullScreenDialog
import com.osscabinet.ossWallet.ui.components.AeonHeroHeader
import com.osscabinet.ossWallet.ui.components.AeonHybridSelector
import com.osscabinet.ossWallet.ui.components.AeonSelectMode
import com.osscabinet.ossWallet.ui.components.AeonSelectionScreen
import com.osscabinet.ossWallet.ui.components.VisualIdentitySection
import com.osscabinet.ossWallet.ui.components.entry.EntrySection
import com.osscabinet.ossWallet.ui.components.icon
import com.osscabinet.ossWallet.ui.screens.accounts.entry.components.CurrencyManagementSection
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.color

/**
 * The same building blocks as [com.osscabinet.ossWallet.ui.screens.accounts.entry.AccountEntryScreen]
 * (hero header, portfolio architecture, account balances, visual identity), embedded as a dialog for
 * configuring a new account during CSV import. Entity creation stays deferred to the final import
 * step — this only returns a fully-populated [EntityMapping], nothing is persisted here.
 */
@Composable
fun ImportAccountConfigDialog(
    csvName: String,
    rowCount: Int,
    initialPrimaryCurrency: String,
    shortlistedCurrencies: List<String>,
    onDismiss: () -> Unit,
    onSave: (EntityMapping) -> Unit
) {
    var name by remember { mutableStateOf(csvName) }
    var structureType by remember { mutableStateOf(AccountStructureType.BANK) }
    var primaryCurrency by remember { mutableStateOf(initialPrimaryCurrency) }
    var allowedCurrencies by remember { mutableStateOf(setOf(initialPrimaryCurrency)) }
    var openingBalances by remember { mutableStateOf(mapOf<String, String>()) }
    var iconName by remember { mutableStateOf(structureType.iconKey) }
    var colorHex by remember { mutableStateOf(structureType.colorHex) }
    var showTypeSelector by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = AppColors.panelSurface,
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth(0.94f)
        ) {
            Column(
                modifier = Modifier
                    .padding(AeonTheme.dimensions.screenPadding)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingLg)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "New Account",
                        style = AppTextStyles.listItemTitle.copy(fontWeight = FontWeight.Bold)
                    )
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = AppColors.contentSecondary,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { onDismiss() }
                    )
                }

                Text(
                    "From CSV — $rowCount row${if (rowCount == 1) "" else "s"}",
                    style = AppTextStyles.caption,
                    color = AppColors.contentSecondary
                )

                AeonHeroHeader(
                    title = name,
                    subtitle = null,
                    iconName = iconName,
                    accentColorHex = colorHex,
                    isLeftAligned = true,
                    onTitleChange = { name = it },
                    onIconClick = null
                )

                EntrySection(title = "PORTFOLIO ARCHITECTURE", icon = Icons.Rounded.AccountTree) {
                    Column(verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingLg)) {
                        Column(verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm)) {
                            Text(
                                text = "STRUCTURE TYPE",
                                style = AppTextStyles.sectionOverline,
                                color = AppColors.contentSecondary.copy(alpha = 0.5f)
                            )
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showTypeSelector = true },
                                shape = RoundedCornerShape(24.dp),
                                color = AppColors.recessedSurface,
                                border = BorderStroke(0.5.dp, AppColors.divider.copy(alpha = 0.2f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = structureType.color.copy(alpha = 0.15f),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = structureType.icon,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp),
                                                tint = structureType.color
                                            )
                                        }
                                    }
                                    Text(
                                        text = structureType.displayName,
                                        style = AppTextStyles.listItemTitle.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        Icons.Rounded.KeyboardArrowDown,
                                        contentDescription = "Select Type",
                                        tint = AppColors.contentSecondary
                                    )
                                }
                            }
                        }

                        CurrencyManagementSection(
                            primaryCurrency = primaryCurrency,
                            onPrimaryCurrencyChange = {
                                primaryCurrency = it
                                allowedCurrencies = allowedCurrencies + it
                            },
                            allowedCurrencies = allowedCurrencies,
                            onAllowedCurrenciesChange = { allowedCurrencies = it },
                            accentColorHex = colorHex,
                            shortlistedCurrencies = shortlistedCurrencies
                        )
                    }
                }

                if (allowedCurrencies.isNotEmpty()) {
                    EntrySection(
                        title = "ACCOUNT BALANCES",
                        icon = Icons.Rounded.AccountBalanceWallet
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)) {
                            Text(
                                "Opening balance before this file, if this account already existed",
                                style = AppTextStyles.caption,
                                color = AppColors.contentSecondary
                            )
                            allowedCurrencies.forEach { currency ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(currency, style = AppTextStyles.overlineLabel)
                                    androidx.compose.material3.OutlinedTextField(
                                        value = openingBalances[currency] ?: "",
                                        onValueChange = {
                                            openingBalances = openingBalances + (currency to it)
                                        },
                                        placeholder = { Text("0.00") },
                                        singleLine = true,
                                        modifier = Modifier.width(120.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                VisualIdentitySection(
                    selectedIconName = iconName,
                    onIconSelected = { iconName = it },
                    accentColorHex = colorHex,
                    onColorSelected = { colorHex = it }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel", color = AppColors.contentSecondary)
                    }
                    Button(
                        onClick = {
                            onSave(
                                EntityMapping(
                                    unknownName = csvName,
                                    existingId = null,
                                    entityType = EntityMapping.EntityType.ACCOUNT,
                                    currencyCode = primaryCurrency,
                                    allowedCurrencies = allowedCurrencies.toList(),
                                    openingBalances = openingBalances,
                                    structureType = structureType.name,
                                    iconName = iconName,
                                    colorHex = colorHex
                                )
                            )
                        },
                        enabled = name.isNotBlank() && primaryCurrency.isNotBlank(),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Text("Save Account", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showTypeSelector) {
        AeonFullScreenDialog(onDismissRequest = { showTypeSelector = false }) {
            AeonSelectionScreen(
                title = "Select Structure Type",
                onBack = { showTypeSelector = false }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .padding(AeonTheme.dimensions.screenPadding)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
                ) {
                    AeonHybridSelector(
                        items = AccountStructureType.entries.toList(),
                        mode = AeonSelectMode.SINGLE,
                        selectedIds = setOf(structureType),
                        onSelectionChange = { ids ->
                            ids.firstOrNull()?.let {
                                structureType = it
                                showTypeSelector = false
                            }
                        },
                        idMapper = { it },
                        labelMapper = { it.displayName },
                        iconMapper = { it.icon },
                        colorMapper = { it.color }
                    )
                }
            }
        }
    }
}
