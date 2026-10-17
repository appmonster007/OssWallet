package com.osscabinet.ossWallet.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.service.search.SearchHit
import com.osscabinet.ossWallet.domain.service.search.SearchSection
import com.osscabinet.ossWallet.ui.components.AeonHazeProvider
import com.osscabinet.ossWallet.ui.components.AeonSearchField
import com.osscabinet.ossWallet.ui.components.AeonTopAppBar
import com.osscabinet.ossWallet.ui.components.AeonTopBarNavTitlePill
import com.osscabinet.ossWallet.ui.components.AeonTopBarSection
import com.osscabinet.ossWallet.ui.components.aeonHazeSource
import com.osscabinet.ossWallet.ui.components.entry.EntrySection
import com.osscabinet.ossWallet.ui.components.getAccountIconByName
import com.osscabinet.ossWallet.ui.components.getIconByName
import com.osscabinet.ossWallet.ui.components.management.AeonListItem
import com.osscabinet.ossWallet.ui.theme.AeonSize
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.HazeLayer
import com.osscabinet.ossWallet.ui.util.formatCurrency
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Where tapping a result goes; each lambda gets the record's id. */
data class GlobalSearchActions(
    val onBackClick: () -> Unit,
    val onTransactionClick: (Long) -> Unit,
    val onAccountClick: (String) -> Unit,
    val onCategoryClick: (Long) -> Unit,
    val onTagClick: (String) -> Unit,
    val onLoanClick: (Long) -> Unit,
    val onBudgetClick: (Long) -> Unit,
    val onGoalsClick: () -> Unit,
    val onTemplateClick: (Long) -> Unit,
    val onPlannedPaymentClick: (Long) -> Unit,
    val onOpenInRecords: (TransactionFilter) -> Unit
)

@Composable
fun GlobalSearchScreen(
    actions: GlobalSearchActions,
    viewModel: GlobalSearchViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focus = remember { FocusRequester() }
    var saving by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    AeonHazeProvider(backgroundColor = AppColors.screenBackground) { hazeLayers ->
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(containerColor = Color.Transparent) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .aeonHazeSource(hazeLayers.states[HazeLayer.Overlay])
                        .statusBarsPadding()
                        .padding(top = AeonSize.D64.value + AeonTheme.dimensions.spacingSm)
                        .imePadding()
                ) {
                    AeonSearchField(
                        query = state.query,
                        onQueryChange = viewModel::setQuery,
                        placeholder = "Search notes, payees, #tags, amounts…",
                        focusRequester = focus,
                        modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding)
                    )
                    SearchChipsRow(state, viewModel)

                    val results = state.results
                    when {
                        results == null -> SearchHint(
                            onExample = viewModel::setQuery,
                            modifier = Modifier.weight(1f)
                        )
                        results.isEmpty -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            if (state.searching) CircularProgressIndicator(strokeWidth = 2.dp)
                            else EmptyResults(state.query.trim())
                        }
                        else -> LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.listPadding),
                            contentPadding = PaddingValues(
                                start = AeonTheme.dimensions.screenPadding, end = AeonTheme.dimensions.screenPadding,
                                top = AeonTheme.dimensions.spacingXs, bottom = innerPadding.calculateBottomPadding() + 32.dp
                            )
                        ) {
                            item(key = "actions") {
                                ResultActions(
                                    onSave = { saving = true },
                                    onOpen = { viewModel.currentRecordsFilter()?.let(actions.onOpenInRecords) }
                                )
                            }
                            results.sections.forEach { section ->
                                val hits = results.hits.getValue(section)
                                item(key = "h-$section") {
                                    SectionLabel(
                                        title = section.title,
                                        count = if (section == SearchSection.TRANSACTIONS) {
                                            if (results.transactionTotal > hits.size) "${hits.size} of ${results.transactionTotal}"
                                            else "${results.transactionTotal}"
                                        } else "${hits.size}"
                                    )
                                }
                                items(hits, key = { "${it.section}-${it.id}" }) { hit ->
                                    HitCard(hit, onClick = { open(hit, actions) })
                                }
                            }
                        }
                    }
                }
            }

            AeonTopAppBar(
                left = AeonTopBarSection(
                    content = { AeonTopBarNavTitlePill(title = "Search", onBackClick = actions.onBackClick) }
                )
            )
        }
    }

    if (saving) SaveFilterDialog(
        suggestion = state.query.trim().take(40),
        onDismiss = { saving = false },
        onSave = { name -> saving = false; viewModel.saveAsFilter(name) }
    )
    state.message?.let { message ->
        LaunchedEffect(message) {
            kotlinx.coroutines.delay(2500)
            viewModel.clearMessage()
        }
        Toast(message)
    }
}

private fun open(hit: SearchHit, a: GlobalSearchActions) {
    when (hit.section) {
        SearchSection.TRANSACTIONS -> hit.id.toLongOrNull()?.let(a.onTransactionClick)
        SearchSection.ACCOUNTS -> a.onAccountClick(hit.id)
        SearchSection.CATEGORIES -> hit.id.toLongOrNull()?.let(a.onCategoryClick)
        SearchSection.TAGS -> a.onTagClick(hit.id)
        SearchSection.LOANS -> hit.id.toLongOrNull()?.let(a.onLoanClick)
        SearchSection.BUDGETS -> hit.id.toLongOrNull()?.let(a.onBudgetClick)
        SearchSection.GOALS -> a.onGoalsClick()
        SearchSection.TEMPLATES -> hit.id.toLongOrNull()?.let(a.onTemplateClick)
        SearchSection.PLANNED_PAYMENTS -> hit.id.toLongOrNull()?.let(a.onPlannedPaymentClick)
    }
}

/** Selected chips use the same accent fill as the Records filter sheet. */
@Composable
private fun chipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = AppColors.accentContainer,
    selectedLabelColor = AppColors.onAccentContainer
)

@Composable
private fun SearchChipsRow(state: GlobalSearchUiState, vm: GlobalSearchViewModel) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = AeonTheme.dimensions.screenPadding, vertical = AeonTheme.dimensions.spacingSm),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(SearchPeriod.entries) { p ->
            FilterChip(selected = state.chips.period == p, onClick = { vm.setPeriod(p) }, colors = chipColors(), label = { Text(p.label) })
        }
        items(SearchKind.entries.filter { it != SearchKind.ALL }) { k ->
            FilterChip(
                selected = state.chips.kind == k,
                onClick = { vm.setKind(if (state.chips.kind == k) SearchKind.ALL else k) },
                colors = chipColors(),
                label = { Text(k.label) }
            )
        }
        items(state.accounts, key = { "a-${it.id}" }) { a ->
            FilterChip(selected = a.id in state.chips.accountIds, onClick = { vm.toggleAccount(a.id) }, colors = chipColors(), label = { Text(a.name) })
        }
    }
}

@Composable
private fun ResultActions(onSave: () -> Unit, onOpen: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AssistChip(
            onClick = onSave,
            label = { Text("Save as filter") },
            leadingIcon = { Icon(Icons.Rounded.BookmarkAdd, contentDescription = null, modifier = Modifier.size(18.dp), tint = AppColors.primary) }
        )
        AssistChip(
            onClick = onOpen,
            label = { Text("Open in Records") },
            leadingIcon = { Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp), tint = AppColors.primary) }
        )
    }
}

/** Same label style as the drawer's section headings: accent overline, a hairline, and a count. */
@Composable
private fun SectionLabel(title: String, count: String) {
    Row(
        Modifier.fillMaxWidth().padding(top = AeonTheme.dimensions.spacingMd),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm)
    ) {
        Text(title.uppercase(), style = AppTextStyles.sectionOverline, color = AppColors.accent)
        Box(Modifier.weight(1f).height(0.5.dp).background(AppColors.outlineVariant))
        Text(count, style = AppTextStyles.caption, color = AppColors.contentSecondary)
    }
}

private fun sectionIcon(section: SearchSection): ImageVector = when (section) {
    SearchSection.TRANSACTIONS -> Icons.Rounded.Wallet
    SearchSection.ACCOUNTS -> Icons.Rounded.AccountBalance
    SearchSection.CATEGORIES -> Icons.Rounded.Sell
    SearchSection.TAGS -> Icons.Rounded.Sell
    SearchSection.LOANS -> Icons.Rounded.AccountBalance
    SearchSection.BUDGETS -> Icons.Rounded.Savings
    SearchSection.GOALS -> Icons.Rounded.Flag
    SearchSection.TEMPLATES -> Icons.Rounded.Bolt
    SearchSection.PLANNED_PAYMENTS -> Icons.Rounded.EventRepeat
}

/** A result as one of the app's list cards: the record's own icon and colour, title, detail, and amount. */
@Composable
private fun HitCard(hit: SearchHit, onClick: () -> Unit) {
    val tint = hit.colorArgb?.let { Color(it) } ?: AppColors.primary
    val icon = when (hit.section) {
        SearchSection.TRANSACTIONS, SearchSection.CATEGORIES -> hit.iconName?.let(::getIconByName) ?: sectionIcon(hit.section)
        SearchSection.ACCOUNTS -> getAccountIconByName(hit.iconName)
        else -> sectionIcon(hit.section)
    }
    // Category and account stay on the main line; the date and note go underneath so nothing is cut short.
    val extra = listOfNotNull(hit.timestamp?.let(::formatDate), hit.detail).joinToString(" · ").ifBlank { null }
    AeonListItem(
        title = hit.title,
        subtitle = hit.subtitle,
        icon = icon,
        iconTint = tint,
        onClick = onClick,
        trailingContent = hit.amount?.let { amount ->
            {
                val color = when (hit.type) {
                    TransactionType.INCOME -> AppColors.positive
                    TransactionType.EXPENSE -> AppColors.negative
                    else -> AppColors.contentPrimary
                }
                val sign = when (hit.type) { TransactionType.INCOME -> "+"; TransactionType.EXPENSE -> "-"; else -> "" }
                Text(
                    sign + formatCurrency(amount.abs(), hit.currency ?: ""),
                    style = AppTextStyles.listItemTitle, color = color, maxLines = 1
                )
            }
        },
        additionalContent = extra?.let { note ->
            {
                Text(note, style = AppTextStyles.caption, color = AppColors.contentSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchHint(onExample: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(androidx.compose.foundation.rememberScrollState())
            .padding(horizontal = AeonTheme.dimensions.screenPadding, vertical = AeonTheme.dimensions.spacingMd),
        verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
    ) {
        EntrySection(title = "TRY SEARCHING FOR", icon = Icons.Rounded.AutoAwesome) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("laptop", "#holiday", "49.99", ">100", "20-50", "rent 1850").forEach { example ->
                    AssistChip(onClick = { onExample(example) }, label = { Text(example) })
                }
            }
            Text(
                "Words match notes, payees, names and categories. #tag finds a tag, and a number finds that amount: " +
                    "49.99, over 100 (>100), between 20 and 50 (20-50). Combine them: rent 1850.",
                style = AppTextStyles.caption, color = AppColors.contentSecondary
            )
        }
        Text(
            "Also searches accounts, categories, tags, loans, budgets, goals, templates and planned payments.",
            style = AppTextStyles.caption, color = AppColors.contentSecondary
        )
    }
}

@Composable
private fun EmptyResults(query: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm)) {
        Icon(
            Icons.Rounded.Search, contentDescription = null,
            modifier = Modifier.size(AeonTheme.dimensions.iconSize3xl),
            tint = AppColors.contentSecondary.copy(alpha = 0.3f)
        )
        Text("Nothing found for “$query”", style = AppTextStyles.listItemTitle, color = AppColors.border)
        Text("Try fewer words, or clear a chip.", style = AppTextStyles.caption, color = AppColors.border)
    }
}

@Composable
private fun SaveFilterDialog(suggestion: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf(suggestion) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save as filter") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true, label = { Text("Name") })
                Text(
                    "Reopen it from Records or Settings → Saved Filters. Words are matched against notes.",
                    style = AppTextStyles.caption, color = AppColors.contentSecondary
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSave(name) }, enabled = name.isNotBlank()) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/** A short-lived line at the bottom (the screen has no Scaffold snackbar host of its own). */
@Composable
private fun Toast(text: String) {
    Box(Modifier.fillMaxSize().padding(bottom = 32.dp), contentAlignment = Alignment.BottomCenter) {
        Surface(shape = RoundedCornerShape(12.dp), color = AppColors.inverseSurface) {
            Text(text, Modifier.padding(horizontal = 16.dp, vertical = 10.dp), color = AppColors.inverseOnSurface, style = AppTextStyles.caption)
        }
    }
}

private fun formatDate(millis: Long): String =
    DateTimeFormatter.ofPattern("MMM d, yyyy").format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))
