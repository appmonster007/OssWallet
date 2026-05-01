package com.osscabinet.ossWallet.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.osscabinet.ossWallet.OssWalletApp
import com.osscabinet.ossWallet.data.local.SettingsDataStore
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class WidgetConfigActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setResult(RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val container = (application as OssWalletApp).container
        val configStore = WidgetConfigStore(this)

        setContent {
            OssWalletTheme {
                WidgetConfigScreen(
                    onConfirm = { config ->
                        lifecycleScope.launch {
                            configStore.saveConfig(appWidgetId, config)
                            // Return RESULT_OK — GlanceAppWidgetReceiver.onUpdate() fires automatically,
                            // which calls provideGlance() and renders the widget with the saved config.
                            val resultValue = Intent().apply {
                                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                            }
                            setResult(RESULT_OK, resultValue)
                            finish()
                        }
                    },
                    onCancel = { finish() },
                    loadAccounts = {
                        val settingsDataStore = SettingsDataStore(this)
                        val profileId = settingsDataStore.currentProfileId.first()
                        container.accountRepository.getAllAccountsSync(profileId)
                    }
                )
            }
        }
    }
}

private enum class WidgetTypeOption(val label: String) {
    NET_WORTH("Net Worth"),
    CASH_FLOW("Cash Flow"),
    ACCOUNT_BALANCE("Account Balance"),
    UPCOMING_PAYMENTS("Upcoming Payments"),
    BUDGET("Budgets")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WidgetConfigScreen(
    onConfirm: (WidgetConfig) -> Unit,
    onCancel: () -> Unit,
    loadAccounts: suspend () -> List<Account>
) {
    var selectedType by remember { mutableStateOf(WidgetTypeOption.NET_WORTH) }
    var accounts by remember { mutableStateOf<List<Account>>(emptyList()) }
    var selectedAccount by remember { mutableStateOf<Account?>(null) }

    LaunchedEffect(Unit) {
        accounts = loadAccounts()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Configure Widget") })
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        val config = when (selectedType) {
                            WidgetTypeOption.NET_WORTH -> WidgetConfig.NetWorth()
                            WidgetTypeOption.CASH_FLOW -> WidgetConfig.CashFlow()
                            WidgetTypeOption.ACCOUNT_BALANCE -> WidgetConfig.AccountBalance(
                                accountId = selectedAccount?.id ?: "",
                                accountName = selectedAccount?.name ?: ""
                            )
                            WidgetTypeOption.UPCOMING_PAYMENTS -> WidgetConfig.UpcomingPayments()
                            WidgetTypeOption.BUDGET -> WidgetConfig.Budget()
                        }
                        onConfirm(config)
                    },
                    enabled = selectedType != WidgetTypeOption.ACCOUNT_BALANCE || selectedAccount != null,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Add Widget")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                Text(
                    text = "Widget Type",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            items(WidgetTypeOption.entries) { type ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedType = type }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = selectedType == type, onClick = { selectedType = type })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = type.label, style = MaterialTheme.typography.bodyLarge)
                }
            }

            if (selectedType == WidgetTypeOption.ACCOUNT_BALANCE) {
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Select Account",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                if (accounts.isEmpty()) {
                    item {
                        Text(
                            text = "No accounts found",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                } else {
                    items(accounts) { account ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedAccount = account }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedAccount?.id == account.id,
                                onClick = { selectedAccount = account }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(text = account.name, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = account.currencyCode,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
