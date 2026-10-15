package com.osscabinet.ossWallet.ui.screens.goals

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.model.Goal
import com.osscabinet.ossWallet.domain.model.GoalProgress
import com.osscabinet.ossWallet.domain.model.GoalStatus
import com.osscabinet.ossWallet.ui.components.AeonFloatingActionButton
import com.osscabinet.ossWallet.ui.components.AeonHazeProvider
import com.osscabinet.ossWallet.ui.components.AeonTopAppBar
import com.osscabinet.ossWallet.ui.components.AeonTopBarNavTitlePill
import com.osscabinet.ossWallet.ui.components.AeonTopBarSection
import com.osscabinet.ossWallet.ui.components.aeonHazeSource
import com.osscabinet.ossWallet.ui.components.cards.types.formatMoney
import com.osscabinet.ossWallet.ui.components.cards.types.statusLabel
import com.osscabinet.ossWallet.ui.theme.AeonSize
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.HazeLayer
import com.osscabinet.ossWallet.ui.theme.WarningColor
import java.math.BigDecimal
import java.text.DateFormat
import java.util.Date

private sealed interface GoalDialog {
    data object Create : GoalDialog
    data class Actions(val goal: Goal) : GoalDialog
    data class Edit(val goal: Goal) : GoalDialog
    data class Contribute(val goal: Goal, val withdraw: Boolean) : GoalDialog
    data class ConfirmDelete(val goal: Goal) : GoalDialog
}

@Composable
fun GoalsScreen(
    onBackClick: () -> Unit,
    viewModel: GoalsViewModel = hiltViewModel()
) {
    val goals by viewModel.goals.collectAsStateWithLifecycle()
    val baseCurrency by viewModel.baseCurrency.collectAsStateWithLifecycle()
    var dialog by remember { mutableStateOf<GoalDialog?>(null) }

    AeonHazeProvider(backgroundColor = AppColors.screenBackground) { hazeLayers ->
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = Color.Transparent,
                floatingActionButton = {
                    AeonFloatingActionButton(
                        onClick = { dialog = GoalDialog.Create },
                        contentDescription = "Add goal"
                    )
                }
            ) { innerPadding ->
                if (goals.isEmpty()) {
                    Box(Modifier.fillMaxSize().statusBarsPadding().padding(32.dp)) {
                        Text(
                            "No savings goals yet.\nTap + to set a target and track your progress.",
                            style = AppTextStyles.caption,
                            color = AppColors.contentSecondary,
                            modifier = Modifier.padding(top = AeonSize.D64.value + 24.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .aeonHazeSource(hazeLayers.states[HazeLayer.Overlay])
                            .statusBarsPadding(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            top = AeonSize.D64.value + AeonTheme.dimensions.screenPadding,
                            bottom = innerPadding.calculateBottomPadding() + 96.dp,
                            start = AeonTheme.dimensions.screenPadding,
                            end = AeonTheme.dimensions.screenPadding
                        ),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(goals, key = { it.goal.id }) { p ->
                            GoalListRow(p, onClick = { dialog = GoalDialog.Actions(p.goal) })
                        }
                    }
                }
            }

            when (val d = dialog) {
                GoalDialog.Create -> GoalEditDialog(
                    existing = null, defaultCurrency = baseCurrency,
                    onSave = { n, t, c, dl -> viewModel.save(n, t, c, dl, null); dialog = null },
                    onDismiss = { dialog = null }
                )
                is GoalDialog.Edit -> GoalEditDialog(
                    existing = d.goal, defaultCurrency = d.goal.currency,
                    onSave = { n, t, c, dl -> viewModel.save(n, t, d.goal.currency, dl, d.goal); dialog = null },
                    onDismiss = { dialog = null }
                )
                is GoalDialog.Actions -> AlertDialog(
                    onDismissRequest = { dialog = null },
                    title = { Text(d.goal.name) },
                    text = {
                        Column {
                            TextButton(onClick = { dialog = GoalDialog.Contribute(d.goal, false) }) { Text("Add money") }
                            TextButton(onClick = { dialog = GoalDialog.Contribute(d.goal, true) }) { Text("Withdraw") }
                            TextButton(onClick = { dialog = GoalDialog.Edit(d.goal) }) { Text("Edit") }
                            TextButton(onClick = { dialog = GoalDialog.ConfirmDelete(d.goal) }) { Text("Delete") }
                        }
                    },
                    confirmButton = {},
                    dismissButton = { TextButton(onClick = { dialog = null }) { Text("Close") } }
                )
                is GoalDialog.Contribute -> ContributeDialog(
                    goal = d.goal, withdraw = d.withdraw,
                    onConfirm = { amount ->
                        viewModel.contribute(d.goal, if (d.withdraw) amount.negate() else amount)
                        dialog = null
                    },
                    onDismiss = { dialog = null }
                )
                is GoalDialog.ConfirmDelete -> AlertDialog(
                    onDismissRequest = { dialog = null },
                    title = { Text("Delete goal?") },
                    text = { Text("\"${d.goal.name}\" and its progress will be removed.") },
                    confirmButton = {
                        TextButton(onClick = { viewModel.delete(d.goal); dialog = null }) { Text("Delete") }
                    },
                    dismissButton = { TextButton(onClick = { dialog = null }) { Text("Cancel") } }
                )
                null -> Unit
            }

            AeonTopAppBar(
                left = AeonTopBarSection(
                    content = { AeonTopBarNavTitlePill(title = "Goals", onBackClick = onBackClick) }
                )
            )
        }
    }
}

@Composable
private fun GoalListRow(p: GoalProgress, onClick: () -> Unit) {
    val g = p.goal
    val color = when (p.status) {
        GoalStatus.COMPLETED -> AppColors.positive
        GoalStatus.BEHIND -> WarningColor
        GoalStatus.OVERDUE -> AppColors.negative
        else -> AppColors.accent
    }
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(g.name, style = AppTextStyles.transactionTitle, color = AppColors.contentPrimary)
            Text(statusLabel(p), style = AppTextStyles.caption, color = color)
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { p.fraction },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = color,
            trackColor = AppColors.divider.copy(alpha = 0.4f)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "${formatMoney(g.savedAmount, g.currency)} of ${formatMoney(g.targetAmount, g.currency)}",
            style = AppTextStyles.caption, color = AppColors.contentSecondary
        )
        p.requiredPerMonth?.let {
            Text(
                "Save ${formatMoney(it, g.currency)} / month to finish on time",
                style = AppTextStyles.caption, color = AppColors.contentSecondary
            )
        }
        g.deadline?.let {
            Text(
                "By ${DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(it))}",
                style = AppTextStyles.caption, color = AppColors.contentSecondary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalEditDialog(
    existing: Goal?,
    defaultCurrency: String,
    onSave: (String, BigDecimal, String, Long?) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var target by remember { mutableStateOf(existing?.targetAmount?.stripTrailingZeros()?.toPlainString() ?: "") }
    var deadline by remember { mutableStateOf(existing?.deadline) }
    var picking by remember { mutableStateOf(false) }
    val amount = target.toBigDecimalOrNull()
    val valid = name.isNotBlank() && amount != null && amount.signum() > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "New goal" else "Edit goal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true)
                OutlinedTextField(
                    target, { target = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Target ($defaultCurrency)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                TextButton(onClick = { picking = true }) {
                    Text(
                        deadline?.let { "Deadline: " + DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(it)) }
                            ?: "Add a deadline (optional)"
                    )
                }
                if (deadline != null) TextButton(onClick = { deadline = null }) { Text("Remove deadline") }
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = { onSave(name, amount!!, defaultCurrency, deadline) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

    if (picking) {
        val state = rememberDatePickerState(initialSelectedDateMillis = deadline)
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = { deadline = state.selectedDateMillis; picking = false }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text("Cancel") } }
        ) { DatePicker(state = state) }
    }
}

@Composable
private fun ContributeDialog(
    goal: Goal,
    withdraw: Boolean,
    onConfirm: (BigDecimal) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    val amount = text.toBigDecimalOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (withdraw) "Withdraw from ${goal.name}" else "Add to ${goal.name}") },
        text = {
            OutlinedTextField(
                text, { text = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Amount (${goal.currency})") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
        },
        confirmButton = {
            TextButton(enabled = amount != null && amount.signum() > 0, onClick = { onConfirm(amount!!) }) {
                Text(if (withdraw) "Withdraw" else "Add")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
