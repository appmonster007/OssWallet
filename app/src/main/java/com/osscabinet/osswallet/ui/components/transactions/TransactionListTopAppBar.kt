package com.osscabinet.ossWallet.ui.components.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CurrencyExchange
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Portrait
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.VerticalAlignBottom
import androidx.compose.material.icons.rounded.VerticalAlignTop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.settings.FastScrollSide
import com.osscabinet.ossWallet.domain.model.settings.TransactionGroupingType
import com.osscabinet.ossWallet.domain.model.settings.TransactionSectionSettings
import com.osscabinet.ossWallet.ui.components.AeonSettingsItem
import com.osscabinet.ossWallet.ui.components.AeonSettingsSection
import com.osscabinet.ossWallet.ui.components.AeonTopAppBar
import com.osscabinet.ossWallet.ui.components.AeonTopBarActionPill
import com.osscabinet.ossWallet.ui.components.AeonTopBarNavTitlePill
import com.osscabinet.ossWallet.ui.components.AeonTopBarSection
import com.osscabinet.ossWallet.ui.components.AppBarIconButton
import com.osscabinet.ossWallet.ui.components.entry.EntrySwitchRow
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListTopAppBar(
    filter: TransactionFilter,
    recordsSettings: TransactionSectionSettings,
    onBackClick: () -> Unit,
    onMenuClick: () -> Unit = {},
    isTopLevel: Boolean = true,
    onUpdateRecordsSettings: (TransactionSectionSettings) -> Unit,
) {
    var showSettingsDialog by remember { mutableStateOf(false) }

    AeonTopAppBar(
        left = AeonTopBarSection(
            content = {
                AeonTopBarNavTitlePill(
                    title = "Records",
                    subtitle = if (filter == TransactionFilter()) "Showing all transactions" else "Filtered results",
                    onBackClick = onBackClick,
                    onMenuClick = onMenuClick,
                    isTopLevel = isTopLevel
                )
            }
        ),
        right = AeonTopBarSection(
            content = {
                AeonTopBarActionPill {
                    AppBarIconButton(
                        icon = Icons.Default.Settings,
                        onClick = { showSettingsDialog = true }
                    )
                }
            }
        )
    )

    if (showSettingsDialog) {
        Dialog(
            onDismissRequest = { showSettingsDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            val configuration = LocalConfiguration.current
            val screenHeight = configuration.screenHeightDp.dp

            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .heightIn(max = screenHeight * 0.85f),
                shape = RoundedCornerShape(32.dp),
                color = AppColors.sheetSurface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "Records Display Settings",
                        style = AppTextStyles.heroAmount.copy(
                            fontWeight = FontWeight.Light,
                            fontSize = 28.sp,
                            color = AppColors.contentPrimary.copy(alpha = 0.8f)
                        ),
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        AeonSettingsSection("DATA & LIFECYCLE") {
                            AeonSettingsItem(
                                title = "Grouping",
                                subtitle = if (recordsSettings.groupBy == TransactionGroupingType.BY_DAY) "By Day" else "By Week",
                                icon = Icons.Rounded.DateRange,
                                trailingContent = {
                                    Icon(
                                        Icons.Rounded.ChevronRight,
                                        contentDescription = null,
                                        tint = AppColors.border,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                },
                                onClick = {
                                    val next =
                                        if (recordsSettings.groupBy == TransactionGroupingType.BY_DAY) TransactionGroupingType.BY_WEEK else TransactionGroupingType.BY_DAY
                                    onUpdateRecordsSettings(recordsSettings.copy(groupBy = next))
                                }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                thickness = 0.5.dp,
                                color = AppColors.divider.copy(alpha = 0.3f)
                            )
                            EntrySwitchRow(
                                label = "Planned Payments",
                                subtitle = "Show upcoming payments in list",
                                icon = Icons.Rounded.NotificationsActive,
                                checked = recordsSettings.showPlannedPayments,
                                onCheckedChange = {
                                    onUpdateRecordsSettings(
                                        recordsSettings.copy(
                                            showPlannedPayments = it
                                        )
                                    )
                                },
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                thickness = 0.5.dp,
                                color = AppColors.divider.copy(alpha = 0.3f)
                            )
                            EntrySwitchRow(
                                label = "Sticky Headers",
                                subtitle = "Transaction headers stick to top",
                                icon = Icons.AutoMirrored.Rounded.Sort,
                                checked = recordsSettings.stickyHeaders,
                                onCheckedChange = {
                                    onUpdateRecordsSettings(
                                        recordsSettings.copy(
                                            stickyHeaders = it
                                        )
                                    )
                                },
                            )
                        }

                        AeonSettingsSection("GROUP SUMMARIES") {
                            EntrySwitchRow(
                                label = "Currency Breakdown",
                                subtitle = "Summary of currencies in group",
                                icon = Icons.Rounded.CurrencyExchange,
                                checked = recordsSettings.showCurrencyBreakdown,
                                onCheckedChange = {
                                    onUpdateRecordsSettings(
                                        recordsSettings.copy(
                                            showCurrencyBreakdown = it
                                        )
                                    )
                                },
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                thickness = 0.5.dp,
                                color = AppColors.divider.copy(alpha = 0.3f)
                            )
                            EntrySwitchRow(
                                label = "Period Overview",
                                subtitle = "Show full period totals regardless of active filter",
                                icon = Icons.Rounded.CalendarMonth,
                                enabled = recordsSettings.showCurrencyBreakdown,
                                checked = recordsSettings.allTxnBreakdown,
                                onCheckedChange = {
                                    onUpdateRecordsSettings(
                                        recordsSettings.copy(
                                            allTxnBreakdown = it
                                        )
                                    )
                                },
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                thickness = 0.5.dp,
                                color = AppColors.divider.copy(alpha = 0.3f)
                            )
                            EntrySwitchRow(
                                label = "Only Active Currencies",
                                subtitle = "Hide zero balance currencies",
                                icon = Icons.AutoMirrored.Rounded.Sort,
                                enabled = recordsSettings.showCurrencyBreakdown && recordsSettings.allTxnBreakdown,
                                checked = !recordsSettings.allTxnBreakdown || recordsSettings.showOnlyActiveCurrencies,
                                onCheckedChange = {
                                    onUpdateRecordsSettings(
                                        recordsSettings.copy(
                                            showOnlyActiveCurrencies = it
                                        )
                                    )
                                },
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                thickness = 0.5.dp,
                                color = AppColors.divider.copy(alpha = 0.3f)
                            )
                            EntrySwitchRow(
                                label = "Opening Balance",
                                subtitle = "Show balance at start of group",
                                icon = Icons.Rounded.VerticalAlignTop,
                                enabled = recordsSettings.allTxnBreakdown,
                                checked = recordsSettings.allTxnBreakdown && recordsSettings.showHeaderOpeningBalance,
                                onCheckedChange = {
                                    onUpdateRecordsSettings(
                                        recordsSettings.copy(
                                            showHeaderOpeningBalance = it
                                        )
                                    )
                                },
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                thickness = 0.5.dp,
                                color = AppColors.divider.copy(alpha = 0.3f)
                            )
                            EntrySwitchRow(
                                label = "Closing Balance",
                                subtitle = "Show balance at end of group",
                                icon = Icons.Rounded.VerticalAlignBottom,
                                enabled = recordsSettings.allTxnBreakdown,
                                checked = recordsSettings.allTxnBreakdown && recordsSettings.showHeaderClosingBalance,
                                onCheckedChange = {
                                    onUpdateRecordsSettings(
                                        recordsSettings.copy(
                                            showHeaderClosingBalance = it
                                        )
                                    )
                                },
                            )
                        }

                        AeonSettingsSection("TRANSACTION DETAILS") {
                            EntrySwitchRow(
                                label = "Show Icon",
                                subtitle = "Category icon visibility",
                                icon = Icons.Rounded.Portrait,
                                checked = recordsSettings.showIcon,
                                onCheckedChange = {
                                    onUpdateRecordsSettings(
                                        recordsSettings.copy(
                                            showIcon = it
                                        )
                                    )
                                },
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                thickness = 0.5.dp,
                                color = AppColors.divider.copy(alpha = 0.3f)
                            )
                            EntrySwitchRow(
                                label = "Show Category",
                                subtitle = "Main classification",
                                icon = Icons.Rounded.Category,
                                checked = recordsSettings.showCategory,
                                onCheckedChange = {
                                    onUpdateRecordsSettings(
                                        recordsSettings.copy(
                                            showCategory = it
                                        )
                                    )
                                },
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                thickness = 0.5.dp,
                                color = AppColors.divider.copy(alpha = 0.3f)
                            )
                            EntrySwitchRow(
                                label = "Show Note",
                                subtitle = "Transaction description",
                                icon = Icons.AutoMirrored.Rounded.Notes,
                                checked = recordsSettings.showNote,
                                onCheckedChange = {
                                    onUpdateRecordsSettings(
                                        recordsSettings.copy(
                                            showNote = it
                                        )
                                    )
                                },
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                thickness = 0.5.dp,
                                color = AppColors.divider.copy(alpha = 0.3f)
                            )
                            EntrySwitchRow(
                                label = "Use Note as Header",
                                subtitle = "Prioritize note over category",
                                icon = Icons.AutoMirrored.Rounded.Sort,
                                enabled = recordsSettings.showNote,
                                checked = recordsSettings.useNoteAsHeader,
                                onCheckedChange = {
                                    onUpdateRecordsSettings(
                                        recordsSettings.copy(
                                            useNoteAsHeader = it
                                        )
                                    )
                                },
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                thickness = 0.5.dp,
                                color = AppColors.divider.copy(alpha = 0.3f)
                            )
                            EntrySwitchRow(
                                label = "Show Account",
                                subtitle = "Source/Target wallet",
                                icon = Icons.Rounded.AccountBalance,
                                checked = recordsSettings.showAccount,
                                onCheckedChange = {
                                    onUpdateRecordsSettings(
                                        recordsSettings.copy(
                                            showAccount = it
                                        )
                                    )
                                },
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                thickness = 0.5.dp,
                                color = AppColors.divider.copy(alpha = 0.3f)
                            )
                            EntrySwitchRow(
                                label = "Show Tags",
                                subtitle = "Custom labels",
                                icon = Icons.AutoMirrored.Rounded.Label,
                                checked = recordsSettings.showTags,
                                onCheckedChange = {
                                    onUpdateRecordsSettings(
                                        recordsSettings.copy(
                                            showTags = it
                                        )
                                    )
                                },
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                thickness = 0.5.dp,
                                color = AppColors.divider.copy(alpha = 0.3f)
                            )
                            EntrySwitchRow(
                                label = "Show Timestamp",
                                subtitle = "Exact time of entry",
                                icon = Icons.Rounded.AccessTime,
                                checked = recordsSettings.showTimestamp,
                                onCheckedChange = {
                                    onUpdateRecordsSettings(
                                        recordsSettings.copy(
                                            showTimestamp = it
                                        )
                                    )
                                },
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                thickness = 0.5.dp,
                                color = AppColors.divider.copy(alpha = 0.3f)
                            )
                            AeonSettingsItem(
                                title = "Fast Scroll Side",
                                subtitle = when (recordsSettings.fastScrollSide) {
                                    FastScrollSide.LEFT -> "Left side"
                                    FastScrollSide.RIGHT -> "Right side"
                                },
                                icon = Icons.Rounded.SwapVert,
                                trailingContent = {
                                    Icon(
                                        Icons.Rounded.ChevronRight,
                                        contentDescription = null,
                                        tint = AppColors.border,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                },
                                onClick = {
                                    val next = when (recordsSettings.fastScrollSide) {
                                        FastScrollSide.RIGHT -> FastScrollSide.LEFT
                                        FastScrollSide.LEFT -> FastScrollSide.RIGHT
                                    }
                                    onUpdateRecordsSettings(recordsSettings.copy(fastScrollSide = next))
                                }
                            )
                        }

                        AeonSettingsSection("RUNNING BALANCES") {
                            EntrySwitchRow(
                                label = "Opening Balance",
                                subtitle = "Balance before transaction",
                                icon = Icons.Rounded.History,
                                checked = recordsSettings.showItemOpeningBalance,
                                onCheckedChange = {
                                    onUpdateRecordsSettings(
                                        recordsSettings.copy(
                                            showItemOpeningBalance = it
                                        )
                                    )
                                },
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                thickness = 0.5.dp,
                                color = AppColors.divider.copy(alpha = 0.3f)
                            )
                            EntrySwitchRow(
                                label = "Closing Balance",
                                subtitle = "Balance after transaction",
                                icon = Icons.Rounded.History,
                                checked = recordsSettings.showItemClosingBalance,
                                onCheckedChange = {
                                    onUpdateRecordsSettings(
                                        recordsSettings.copy(
                                            showItemClosingBalance = it
                                        )
                                    )
                                },
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        TextButton(
                            onClick = { showSettingsDialog = false }
                        ) {
                            Text(
                                "Close",
                                style = AppTextStyles.listItemTitle.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = AppColors.primary
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TransactionListTopAppBarPreview() {
    OssWalletTheme {
        TransactionListTopAppBar(
            filter = TransactionFilter(),
            recordsSettings = TransactionSectionSettings(),
            onBackClick = {},
            onUpdateRecordsSettings = {}
        )
    }
}
