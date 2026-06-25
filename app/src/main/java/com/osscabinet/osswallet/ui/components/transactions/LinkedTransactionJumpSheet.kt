package com.osscabinet.ossWallet.ui.components.transactions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.CashFlowType
import com.osscabinet.ossWallet.domain.model.ConfirmedTransactionUiModel
import com.osscabinet.ossWallet.ui.components.getIconByName
import com.osscabinet.ossWallet.ui.screens.transactions.model.TransactionListItem
import com.osscabinet.ossWallet.ui.theme.AeonSize
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkedTransactionJumpSheet(
    transactions: List<TransactionListItem>,
    onJump: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = AppColors.cardSurfaceProminent,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = AeonSize.D32.value)
        ) {
            Text(
                text = "SELECT LINKED TRANSACTION",
                style = AppTextStyles.sectionOverline,
                color = AppColors.contentSecondary,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )

            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                transactions.forEachIndexed { index, item ->
                    val record = item.record
                    AeonTransactionItem(
                        category = record.categoryName,
                        account = record.accountName,
                        amount = item.formattedAmount,
                        convertedAmount = item.formattedConvertedAmount,
                        balanceBefore = "",
                        balanceAfter = "",
                        type = if (record.amount.signum() >= 0) CashFlowType.INCOME else CashFlowType.EXPENSE,
                        timestamp = item.formattedDate,
                        note = record.note,
                        tags = record.tags,
                        icon = getIconByName(record.categoryIcon),
                        iconBg = Color(record.categoryColor),
                        showDividers = index < transactions.size - 1,
                        onClick = {
                            if (record is ConfirmedTransactionUiModel) {
                                onJump(record.item.transaction.id)
                            }
                        }
                    )
                }
            }
        }
    }
}
