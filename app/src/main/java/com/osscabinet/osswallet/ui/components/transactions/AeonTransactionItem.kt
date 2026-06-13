package com.osscabinet.ossWallet.ui.components.transactions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.CashFlowType
import com.osscabinet.ossWallet.domain.model.TagUiModel
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.ui.theme.AccentBlue
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.ColorOnDark
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.PositiveAmountColor
import com.osscabinet.ossWallet.ui.theme.scaledSp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AeonTransactionItem(
    category: String,
    account: String,
    amount: String,
    modifier: Modifier = Modifier,
    convertedAmount: String? = null,
    balanceBefore: String,
    balanceAfter: String,
    type: CashFlowType,
    timestamp: String,
    note: String? = null,
    tags: List<TagUiModel> = emptyList(),
    icon: ImageVector? = null,
    iconBg: Color? = null,
    useNoteAsHeader: Boolean = false,
    showOpeningBalance: Boolean = false,
    showClosingBalance: Boolean = true,
    showDividers: Boolean = true,
    status: TransactionStatus = TransactionStatus.CLEARED,
    showAccount: Boolean = true,
    showCategory: Boolean = true,
    showNote: Boolean = true,
    showTags: Boolean = true,
    showTimestamp: Boolean = true,
    showIcon: Boolean = true,
    containerColor: Color = AppColors.screenBackground,
    isPlanned: Boolean = false,
    onClick: () -> Unit
) {
    val uiScale = AeonTheme.uiScale
    val isNoteHeader = useNoteAsHeader && !note.isNullOrBlank()
    val headerText = remember(isNoteHeader, showNote, note, showCategory, category) {
        if (isNoteHeader) (if (showNote) note else if (showCategory) category else "")
        else (if (showCategory) category else if (showNote) note else "")
    }
    val bottomText = remember(isNoteHeader, showCategory, showNote, category, note) {
        if (isNoteHeader) (if (showCategory) category else "")
        else (if (showNote) note else "")
    }

    val itemContainerColor = containerColor

    val dashedBorder = if (isPlanned) {
        val borderColor = AppColors.primary
        val cornerRadius = AeonTheme.dimensions.componentCornerRadius
        Modifier.drawWithContent {
            drawContent()
            val strokeWidth = 1.dp.toPx()
            val dashWidth = 6.dp.toPx()
            val dashGap = 4.dp.toPx()
            drawRoundRect(
                color = borderColor,
                style = Stroke(
                    width = strokeWidth,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashWidth, dashGap), 0f)
                ),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius.toPx())
            )
        }
    } else Modifier

    val outerPadding = 6.dp

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = containerColor,
        shape = RectangleShape
    ) {
        Column(
            modifier = if (isPlanned) {
                Modifier
                    .padding(horizontal = outerPadding, vertical = 4.dp)
                    .then(dashedBorder)
                    .padding(
                        horizontal = AeonTheme.dimensions.listPadding - outerPadding,
                        vertical = AeonTheme.dimensions.spacingSm
                    )
            } else {
                Modifier.padding(
                    horizontal = AeonTheme.dimensions.listPadding,
                    vertical = AeonTheme.dimensions.spacingSm
                )
            }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                    if (showIcon) {
                        Box(modifier = Modifier.size(AeonTheme.dimensions.iconSizeLg)) {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                shape = CircleShape,
                                color = (iconBg ?: AppColors.primaryContainer).copy(
                                    alpha = 0.2f
                                )
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        icon ?: Icons.Rounded.Category,
                                        contentDescription = null,
                                        tint = iconBg ?: AppColors.primary,
                                        modifier = Modifier.size(AeonTheme.dimensions.iconSizeSm)
                                    )
                                }
                            }

                            // Status indicator
                            if (status != TransactionStatus.UNCLEARED || isPlanned) {
                                Surface(
                                    modifier = Modifier
                                        .size(AeonTheme.dimensions.iconSizeSm)
                                        .align(Alignment.BottomStart)
                                        .offset(x = (-4).dp, y = 4.dp),
                                    shape = CircleShape,
                                    color = when {
                                        isPlanned -> AppColors.tertiary
                                        status == TransactionStatus.CLEARED -> PositiveAmountColor
                                        status == TransactionStatus.RECONCILE -> AccentBlue
                                        else -> AppColors.transparent
                                    },
                                    border = BorderStroke(1.5.dp, itemContainerColor)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = when {
                                                isPlanned -> Icons.Rounded.Schedule
                                                status == TransactionStatus.CLEARED -> Icons.Rounded.Check
                                                else -> Icons.Rounded.Sync
                                            },
                                            contentDescription = null,
                                            tint = ColorOnDark,
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(AeonTheme.dimensions.spacingSm))
                    }

                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingXs)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            if (!headerText.isNullOrEmpty()) {
                                Text(
                                    headerText,
                                    style = AppTextStyles.transactionTitle.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = scaledSp(12f, uiScale)
                                    ),
                                    maxLines = if (isNoteHeader) Int.MAX_VALUE else 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (showAccount) {
                                Text(
                                    account,
                                    style = AppTextStyles.transactionSwipeAction.copy(
                                        fontSize = scaledSp(10f, uiScale),
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = AppColors.contentSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (!bottomText.isNullOrBlank()) {
                                Text(
                                    bottomText,
                                    style = AppTextStyles.caption.copy(
                                        fontSize = scaledSp(11f, uiScale)
                                    ),
                                    color = AppColors.contentSecondary,
                                    modifier = Modifier.padding(top = 2.dp),
                                    maxLines = if (!isNoteHeader) Int.MAX_VALUE else 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            val amountColor =
                                if (type == CashFlowType.INCOME) AppColors.positive else AppColors.negative
                            Text(
                                text = amount,
                                style = AppTextStyles.transactionAmount.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = scaledSp(15f, uiScale),
                                    color = amountColor
                                )
                            )

                            if (convertedAmount != null) {
                                Text(
                                    text = convertedAmount,
                                    style = AppTextStyles.transactionAmountMeta.copy(
                                        fontSize = 9.sp
                                    ),
                                    color = amountColor.copy(alpha = 0.8f)
                                )
                            }

                            if (showOpeningBalance || showClosingBalance) {
                                val balanceText = when {
                                    showOpeningBalance && showClosingBalance -> "$balanceBefore → $balanceAfter"
                                    showOpeningBalance -> balanceBefore
                                    else -> balanceAfter
                                }
                                Text(
                                    balanceText,
                                    style = AppTextStyles.transactionAmountMeta,
                                    color = AppColors.contentSecondary.copy(alpha = 0.8f)
                                )
                            }

                            if (showTimestamp) {
                                Text(
                                    timestamp,
                                    style = AppTextStyles.transactionAmountMeta,
                                    color = AppColors.contentSecondary.copy(alpha = 0.8f),
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }

                if (showTags && tags.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.padding(top = AeonTheme.dimensions.spacingSm),
                        horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingXs),
                        verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingXs)
                    ) {
                        tags.forEach { tag ->
                            val tagColor =
                                if (tag.color != -1) Color(tag.color) else AppColors.secondaryContainer
                            Surface(
                                shape = RoundedCornerShape(AeonTheme.dimensions.spacingXs),
                                color = tagColor.copy(alpha = 0.2f),
                            ) {
                                Text(
                                    text = tag.name,
                                    style = AppTextStyles.transactionSwipeAction.copy(
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (tag.color != -1) tagColor else AppColors.onSecondaryContainer
                                    ),
                                    modifier = Modifier.padding(
                                        horizontal = AeonTheme.dimensions.spacingXs,
                                        vertical = 2.dp
                                    )
                                )
                            }
                        }
                    }
                }
        }

        if (showDividers) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = AeonTheme.dimensions.listPadding),
                thickness = 0.5.dp,
                color = AppColors.divider.copy(alpha = 0.5f)
            )
        }
    }
}

@Preview(showBackground = true, name = "Regular Transaction")
@Composable
fun AeonTransactionItemRegularPreview() {
    OssWalletTheme {
        AeonTransactionItem(
            category = "Food & Drinks",
            account = "Main Wallet",
            amount = "-$15.50",
            balanceBefore = "$100.00",
            balanceAfter = "$84.50",
            type = CashFlowType.EXPENSE,
            timestamp = "10:30 AM",
            note = "Lunch at Subway",
            tags = listOf(
                TagUiModel("Lunch", -1),
                TagUiModel("Work", -1)
            ),
            icon = Icons.Rounded.Category,
            iconBg = Color(0xFF4CAF50),
            onClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Planned Payment")
@Composable
fun AeonTransactionItemPlannedPreview() {
    OssWalletTheme {
        Surface(
            color = AppColors.surfaceContainer, // Contrast background
            modifier = Modifier.padding(16.dp)
        ) {
            AeonTransactionItem(
                category = "Subscriptions",
                account = "Credit Card",
                amount = "-$12.99",
                balanceBefore = "$500.00",
                balanceAfter = "$487.01",
                type = CashFlowType.EXPENSE,
                timestamp = "Tomorrow",
                note = "Netflix Subscription",
                isPlanned = true,
                icon = Icons.Rounded.Category,
                iconBg = Color(0xFFE91E63),
                onClick = {}
            )
        }
    }
}
