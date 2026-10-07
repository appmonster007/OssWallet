package com.osscabinet.ossWallet.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme

// Previews for the intermediary config screens shown by Android before a
// pinned widget is placed on the homescreen (APPWIDGET_CONFIGURE activities).

@Composable
@Preview(name = "Net Worth config", showBackground = true)
private fun NetWorthWidgetConfigScreenPreview() {
    OssWalletTheme {
        var config by remember { mutableStateOf(NetWorthWidgetConfig()) }
        NetWorthWidgetConfigScreen(
            config = config,
            onConfigChange = { config = it },
            onSave = {},
            onBack = {}
        )
    }
}

@Composable
@Preview(name = "Cash Flow config", showBackground = true)
private fun CashFlowWidgetConfigScreenPreview() {
    OssWalletTheme {
        var config by remember { mutableStateOf(CashFlowWidgetConfig()) }
        CashFlowWidgetConfigScreen(
            config = config,
            onConfigChange = { config = it },
            onSave = {},
            onBack = {}
        )
    }
}

@Composable
@Preview(name = "Recent Transactions config", showBackground = true)
private fun RecentTransactionsWidgetConfigScreenPreview() {
    OssWalletTheme {
        var config by remember { mutableStateOf(RecentTransactionsWidgetConfig()) }
        RecentTransactionsWidgetConfigScreen(
            config = config,
            onConfigChange = { config = it },
            onSave = {},
            onBack = {}
        )
    }
}
