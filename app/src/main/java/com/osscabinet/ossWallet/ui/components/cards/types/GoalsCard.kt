package com.osscabinet.ossWallet.ui.components.cards.types

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.CardUpdateHandler
import com.osscabinet.ossWallet.domain.model.GoalProgress
import com.osscabinet.ossWallet.domain.model.GoalStatus
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.settings.GoalsCardConfig
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.ui.components.cards.AeonExpressiveCard
import com.osscabinet.ossWallet.ui.components.cards.AeonInteractionCallbacks
import com.osscabinet.ossWallet.ui.components.cards.CardCallbacks
import com.osscabinet.ossWallet.ui.components.cards.CardLayoutProps
import com.osscabinet.ossWallet.ui.components.cards.CardWithCallbacks
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.WarningColor
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat

@Composable
fun GoalsCard(
    items: List<GoalProgress>,
    config: GoalsCardConfig,
    callbacks: CardCallbacks.Goals,
    layoutProps: CardLayoutProps = CardLayoutProps(),
) {
    AeonExpressiveCard(
        title = config.title,
        icon = Icons.Rounded.Flag,
        modifier = layoutProps.modifier,
        headerModifier = layoutProps.headerModifier,
        onClick = callbacks.onSeeAll,
        filterEnabled = false,
        onDelete = callbacks.onDelete,
    ) {
        if (items.isEmpty()) {
            Text(
                text = "No goals yet — tap to set a savings target",
                style = AppTextStyles.caption,
                color = AppColors.contentSecondary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items.forEach { GoalRow(it, onClick = { callbacks.onGoalClick(it.goal.id) }) }
            }
        }
    }
}

@Composable
private fun GoalRow(progress: GoalProgress, onClick: () -> Unit) {
    val goal = progress.goal
    Column(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(goal.name, style = AppTextStyles.transactionTitle, color = AppColors.contentPrimary)
            Text(
                statusLabel(progress),
                style = AppTextStyles.caption,
                color = statusColor(progress.status)
            )
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { progress.fraction },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = statusColor(progress.status),
            trackColor = AppColors.divider.copy(alpha = 0.4f)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "${formatMoney(goal.savedAmount, goal.currency)} of ${formatMoney(goal.targetAmount, goal.currency)}",
            style = AppTextStyles.caption,
            color = AppColors.contentSecondary
        )
    }
}

@Composable
private fun statusColor(status: GoalStatus): Color = when (status) {
    GoalStatus.COMPLETED -> AppColors.positive
    GoalStatus.ON_TRACK, GoalStatus.NO_DEADLINE -> AppColors.accent
    GoalStatus.BEHIND -> WarningColor
    GoalStatus.OVERDUE -> AppColors.negative
}

internal fun statusLabel(p: GoalProgress): String = when (p.status) {
    GoalStatus.COMPLETED -> "Reached"
    GoalStatus.OVERDUE -> "Overdue"
    GoalStatus.BEHIND -> "Behind · ${p.daysLeft}d left"
    GoalStatus.ON_TRACK -> "${p.daysLeft}d left"
    GoalStatus.NO_DEADLINE -> "${(p.fraction * 100).toInt()}%"
}

internal fun formatMoney(amount: BigDecimal, currency: String): String {
    val nf = NumberFormat.getNumberInstance().apply {
        minimumFractionDigits = CurrencyService.precision
        maximumFractionDigits = CurrencyService.precision
    }
    return CurrencyService.getCurrencySymbol(currency) +
        nf.format(amount.setScale(CurrencyService.precision, RoundingMode.HALF_UP))
}

fun mapGoals(
    config: GoalsCardConfig,
    renderData: CardRenderData.Goals,
    input: CardInput,
    updateHandler: CardUpdateHandler,
    onDelete: ((String) -> Unit)?,
    callbacks: AeonInteractionCallbacks,
) = CardWithCallbacks.Goals(
    renderData = renderData,
    callbacks = CardCallbacks.Goals(
        onUpdate = { newConfig -> updateHandler.updateCardGoalsConfig(config.cardId, newConfig) },
        onGoalClick = callbacks.goalActions.onClick,
        onSeeAll = callbacks.goalActions.onSeeAll,
        onDelete = onDelete?.let { { it(config.cardId) } }
    ),
    filterRules = config.filterRules,
    baseFilter = input.filter,
    filterSheetEnabled = false
)
