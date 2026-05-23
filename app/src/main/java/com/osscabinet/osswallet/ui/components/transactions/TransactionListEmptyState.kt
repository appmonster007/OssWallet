package com.osscabinet.ossWallet.ui.components.transactions

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme

@Composable
fun TransactionListEmptyState(
    modifier: Modifier = Modifier,
    message: String = "No records found"
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = AppTextStyles.listItemTitle,
            color = AppColors.border
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TransactionListEmptyStatePreview() {
    OssWalletTheme {
        Surface {
            TransactionListEmptyState()
        }
    }
}
