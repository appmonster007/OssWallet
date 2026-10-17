package com.osscabinet.ossWallet.ui.screens.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
                    if (state.results?.isEmpty == false) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = AeonTheme.dimensions.screenPadding),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { saving = true }) {
                                Icon(Icons.Rounded.BookmarkAdd, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                                Text("Save as filter")
                            }
                            TextButton(onClick = { viewModel.currentRecordsFilter()?.let(actions.onOpenInRecords) }) {
                                Text("Open in Records")
                            }
                        }
                    }

                    val results = state.results
                    when {
                        results == null -> SearchHint(modifier = Modifier.weight(1f))
                        results.isEmpty -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            if (state.searching) CircularProgressIndicator(strokeWidth = 2.dp)
                            else Text("Nothing found for “${state.query.trim()}”", style = AppTextStyles.caption, color = AppColors.contentSecondary)
                        }
                        else -> LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentPadding = PaddingValues(
                                start = AeonTheme.dimensions.screenPadding, end = AeonTheme.dimensions.screenPadding,
                                top = 8.dp, bottom = innerPadding.calculateBottomPadding() + 24.dp
                            )
                        ) {
                            results.sections.forEach { section ->
                                val hits = results.hits.getValue(section)
                                item(key = "h-$section") {
                                    SectionHeader(
                                        title = section.title,
                                        trailing = if (section == SearchSection.TRANSACTIONS && results.transactionTotal > hits.size)
                                            "${hits.size} of ${results.transactionTotal}" else null
                                    )
                                }
                                items(hits, key = { "${it.section}-${it.id}" }) { hit ->
                                    HitRow(hit, onClick = { open(hit, actions) })
                                    HorizontalDivider(color = AppColors.outlineVariant.copy(alpha = 0.3f))
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
    }
    state.message?.let { Toast(it) }
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

@Composable
private fun SearchChipsRow(state: GlobalSearchUiState, vm: GlobalSearchViewModel) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = AeonTheme.dimensions.screenPadding, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(SearchPeriod.entries) { p ->
            FilterChip(selected = state.chips.period == p, onClick = { vm.setPeriod(p) }, label = { Text(p.label) })
        }
        items(SearchKind.entries.filter { it != SearchKind.ALL }) { k ->
            FilterChip(
                selected = state.chips.kind == k,
                onClick = { vm.setKind(if (state.chips.kind == k) SearchKind.ALL else k) },
                label = { Text(k.label) }
            )
        }
        items(state.accounts, key = { "a-${it.id}" }) { a ->
            FilterChip(selected = a.id in state.chips.accountIds, onClick = { vm.toggleAccount(a.id) }, label = { Text(a.name) })
        }
    }
}

@Composable
private fun SectionHeader(title: String, trailing: String?) {
    Row(
        Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title.uppercase(), style = AppTextStyles.caption, color = AppColors.contentSecondary, modifier = Modifier.weight(1f))
        trailing?.let { Text(it, style = AppTextStyles.caption, color = AppColors.contentSecondary) }
    }
}

@Composable
private fun HitRow(hit: SearchHit, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(hit.title, style = AppTextStyles.transactionTitle, color = AppColors.contentPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val line = listOfNotNull(hit.timestamp?.let(::formatDate), hit.subtitle).joinToString(" · ")
            if (line.isNotBlank()) Text(line, style = AppTextStyles.caption, color = AppColors.contentSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        hit.amount?.let { amount ->
            val color = when (hit.type) {
                TransactionType.INCOME -> AppColors.positive
                TransactionType.EXPENSE -> AppColors.negative
                else -> AppColors.contentPrimary
            }
            Text(
                formatCurrency(amount.abs(), hit.currency ?: ""),
                style = AppTextStyles.transactionTitle, color = color, maxLines = 1
            )
        }
    }
}

@Composable
private fun SearchHint(modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(AeonTheme.dimensions.screenPadding),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("Search everything", style = AppTextStyles.transactionTitle, color = AppColors.contentPrimary)
        listOf(
            "laptop" to "a word in a note, payee, name or category",
            "#holiday" to "a tag",
            "49.99" to "an amount",
            ">100  or  20-50" to "amounts over 100, or between 20 and 50",
            "rent 1200" to "words and an amount together"
        ).forEach { (example, meaning) ->
            Text("$example  —  $meaning", style = AppTextStyles.caption, color = AppColors.contentSecondary)
        }
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
        androidx.compose.material3.Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            color = AppColors.inverseSurface
        ) {
            Text(text, Modifier.padding(horizontal = 16.dp, vertical = 10.dp), color = AppColors.inverseOnSurface, style = AppTextStyles.caption)
        }
    }
}

private fun formatDate(millis: Long): String =
    DateTimeFormatter.ofPattern("MMM d, yyyy").format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))
