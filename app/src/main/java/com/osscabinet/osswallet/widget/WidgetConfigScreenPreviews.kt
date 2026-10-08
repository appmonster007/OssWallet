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

// Cash Flow / Recent Transactions / Phase 2-3 widget previews were removed when those widgets
// were shelved (see shelved-widgets/ at the repo root) — only Net Worth remains wired.
