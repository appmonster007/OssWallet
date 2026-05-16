package com.osscabinet.ossWallet.ui.components.transactions

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.osscabinet.ossWallet.ui.theme.AppColors

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
            style = MaterialTheme.typography.bodyLarge,
            color = AppColors.border
        )
    }
}
