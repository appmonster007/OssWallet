package com.osscabinet.ossWallet.ui.components.entry

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.osscabinet.ossWallet.ui.components.CalculatorNumpad
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.ThemePreviews

/**
 * Calculator bottom sheet using the full CalculatorNumpad component.
 * Following the "Architect Slate" design specification.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AeonCalculatorSheet(
    currentAmount: String,
    onAmountChanged: (String) -> Unit,
    onDismiss: () -> Unit,
    title: String? = null
) {
    val dims = AeonTheme.dimensions

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(
            topStart = dims.dialogCornerRadius,
            topEnd = dims.dialogCornerRadius
        ),
        containerColor = AppColors.cardSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = AppColors.divider.copy(alpha = 0.4f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(horizontal = dims.spacingXl)
                .padding(bottom = dims.spacing2xl)
        ) {
            if (title != null) {
                Text(
                    text = title,
                    style = AppTextStyles.cardSectionTitle,
                    modifier = Modifier.padding(bottom = dims.spacingLg, start = dims.spacingSm)
                )
            }

            CalculatorNumpad(
                amount = currentAmount,
                onAmountChange = { amount ->
                    onAmountChanged(amount)
                },
                onDone = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@ThemePreviews
@Composable
fun AeonCalculatorSheetPreview() {
    OssWalletTheme {
        AeonCalculatorSheet(
            currentAmount = "1234.56",
            onAmountChanged = {},
            onDismiss = {},
            title = "Enter Amount"
        )
    }
}
