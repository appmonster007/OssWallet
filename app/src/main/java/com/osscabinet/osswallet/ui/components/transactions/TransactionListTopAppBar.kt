package com.osscabinet.ossWallet.ui.components.transactions

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.osscabinet.ossWallet.data.local.TransactionGroupingType
import com.osscabinet.ossWallet.data.local.TransactionSectionSettings
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.ui.theme.AppColors

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
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (filter == TransactionFilter()) "Showing all transactions" else "Filtered results",
                    style = MaterialTheme.typography.labelSmall,
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
                }
            }
        }
    )
}
