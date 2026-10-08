package com.osscabinet.ossWallet.widget

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
class AccountsWidgetConfigActivity : BaseWidgetConfigActivity<AccountsWidgetConfig>() {

    override val prefsStore by lazy { AccountsWidgetPrefsStore(applicationContext) }
    override val defaultConfig = AccountsWidgetConfig()

    @Composable
    override fun Content(
        config: AccountsWidgetConfig,
        onConfigChange: (AccountsWidgetConfig) -> Unit,
        onSave: () -> Unit,
        onBack: () -> Unit
    ) {
        AccountsWidgetConfigScreen(config, onConfigChange, onSave, onBack)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AccountsWidgetConfigScreen(
    config: AccountsWidgetConfig,
    onConfigChange: (AccountsWidgetConfig) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    WidgetConfigScreen(
        title = "Accounts Widget",
        onBack = onBack,
        onSave = onSave
    ) {
        WidgetConfigFiltersSection(
            filter = config.filter,
            onFilterChange = { onConfigChange(config.copy(filter = it)) },
            onClose = onBack
        )
    }
}
