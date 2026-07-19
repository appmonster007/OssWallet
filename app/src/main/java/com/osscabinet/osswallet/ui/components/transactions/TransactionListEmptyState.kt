package com.osscabinet.ossWallet.ui.components.transactions

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme

@Composable
fun TransactionListEmptyState(
    modifier: Modifier = Modifier,
    message: String = "No records found",
    isFutureExcluded: Boolean = false,
    onIncludeFutureClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = message,
                style = AppTextStyles.listItemTitle,
                color = AppColors.contentSecondary
            )

            if (isFutureExcluded && onIncludeFutureClick != null) {
                androidx.compose.material3.TextButton(onClick = onIncludeFutureClick) {
                    Text(
                        "Include future transactions?",
                        style = AppTextStyles.caption.copy(color = AppColors.primary)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TransactionListEmptyStatePreview() {
    OssWalletTheme {
        Surface {
            TransactionListEmptyState(isFutureExcluded = true, onIncludeFutureClick = {})
        }
    }
}
