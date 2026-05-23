package com.osscabinet.ossWallet.ui.components.transactions

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.settings.TransactionGroupingType
import com.osscabinet.ossWallet.domain.model.settings.TransactionSectionSettings
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
    var showMoreMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Column {
                Text(
                    "Records",
                    style = AppTextStyles.screenTitle,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (filter == TransactionFilter()) "Showing all transactions" else "Filtered results",
                    style = AppTextStyles.transactionSwipeAction,
                    color = AppColors.border
                )
            }
        },
        navigationIcon = {
            if (isTopLevel) {
                IconButton(onClick = onMenuClick) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu")
                }
            } else {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        },
        actions = {
            Box {
                IconButton(onClick = { showMoreMenu = true }) {
                    Icon(Icons.Default.MoreHoriz, contentDescription = "More")
                }
                DropdownMenu(
                    expanded = showMoreMenu,
                    onDismissRequest = { showMoreMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Group by Day") },
                        leadingIcon = {
                            RadioButton(
                                selected = recordsSettings.groupBy == TransactionGroupingType.BY_DAY,
                                onClick = null
                            )
                        },
                        onClick = {
                            onUpdateRecordsSettings(recordsSettings.copy(groupBy = TransactionGroupingType.BY_DAY))
                            showMoreMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Group by Week") },
                        leadingIcon = {
                            RadioButton(
                                selected = recordsSettings.groupBy == TransactionGroupingType.BY_WEEK,
                                onClick = null
                            )
                        },
                        onClick = {
                            onUpdateRecordsSettings(recordsSettings.copy(groupBy = TransactionGroupingType.BY_WEEK))
                            showMoreMenu = false
                        }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Use Note as Header") },
                        leadingIcon = {
                            Checkbox(
                                checked = recordsSettings.useNoteAsHeader,
                                onCheckedChange = null
                            )
                        },
                        onClick = {
                            onUpdateRecordsSettings(recordsSettings.copy(useNoteAsHeader = !recordsSettings.useNoteAsHeader))
                            showMoreMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Show Opening Balance") },
                        leadingIcon = {
                            Checkbox(
                                checked = recordsSettings.showOpeningBalance,
                                onCheckedChange = null
                            )
                        },
                        onClick = {
                            onUpdateRecordsSettings(recordsSettings.copy(showOpeningBalance = !recordsSettings.showOpeningBalance))
                            showMoreMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Show Closing Balance") },
                        leadingIcon = {
                            Checkbox(
                                checked = recordsSettings.showClosingBalance,
                                onCheckedChange = null
                            )
                        },
                        onClick = {
                            onUpdateRecordsSettings(recordsSettings.copy(showClosingBalance = !recordsSettings.showClosingBalance))
                            showMoreMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Show Only Active Currencies") },
                        leadingIcon = {
                            Checkbox(
                                checked = recordsSettings.showOnlyActiveCurrencies,
                                onCheckedChange = null
                            )
                        },
                        onClick = {
                            onUpdateRecordsSettings(recordsSettings.copy(showOnlyActiveCurrencies = !recordsSettings.showOnlyActiveCurrencies))
                            showMoreMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Exclude Future Transactions") },
                        leadingIcon = {
                            Checkbox(
                                checked = recordsSettings.excludeFutureTransactions,
                                onCheckedChange = null
                            )
                        },
                        onClick = {
                            onUpdateRecordsSettings(recordsSettings.copy(excludeFutureTransactions = !recordsSettings.excludeFutureTransactions))
                            showMoreMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Show Currency Breakdown") },
                        leadingIcon = {
                            Checkbox(
                                checked = recordsSettings.showCurrencyBreakdown,
                                onCheckedChange = null
                            )
                        },
                        onClick = {
                            onUpdateRecordsSettings(recordsSettings.copy(showCurrencyBreakdown = !recordsSettings.showCurrencyBreakdown))
                            showMoreMenu = false
                        }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Show Account") },
                        leadingIcon = {
                            Checkbox(
                                checked = recordsSettings.showAccount,
                                onCheckedChange = null
                            )
                        },
                        onClick = {
                            onUpdateRecordsSettings(recordsSettings.copy(showAccount = !recordsSettings.showAccount))
                            showMoreMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Show Category") },
                        leadingIcon = {
                            Checkbox(
                                checked = recordsSettings.showCategory,
                                onCheckedChange = null
                            )
                        },
                        onClick = {
                            onUpdateRecordsSettings(recordsSettings.copy(showCategory = !recordsSettings.showCategory))
                            showMoreMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Show Note") },
                        leadingIcon = {
                            Checkbox(
                                checked = recordsSettings.showNote,
                                onCheckedChange = null
                            )
                        },
                        onClick = {
                            onUpdateRecordsSettings(recordsSettings.copy(showNote = !recordsSettings.showNote))
                            showMoreMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Show Tags") },
                        leadingIcon = {
                            Checkbox(
                                checked = recordsSettings.showTags,
                                onCheckedChange = null
                            )
                        },
                        onClick = {
                            onUpdateRecordsSettings(recordsSettings.copy(showTags = !recordsSettings.showTags))
                            showMoreMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Show Timestamp") },
                        leadingIcon = {
                            Checkbox(
                                checked = recordsSettings.showTimestamp,
                                onCheckedChange = null
                            )
                        },
                        onClick = {
                            onUpdateRecordsSettings(recordsSettings.copy(showTimestamp = !recordsSettings.showTimestamp))
                            showMoreMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Show Icon") },
                        leadingIcon = {
                            Checkbox(
                                checked = recordsSettings.showIcon,
                                onCheckedChange = null
                            )
                        },
                        onClick = {
                            onUpdateRecordsSettings(recordsSettings.copy(showIcon = !recordsSettings.showIcon))
                            showMoreMenu = false
                        }
                    )
                }
            }
        }
    )
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
