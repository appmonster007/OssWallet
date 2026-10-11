package com.osscabinet.ossWallet.widget

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.di.WidgetDataLoaderEntryPoint
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.forAccount
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first

/**
 * Account Hero is account-pinned per instance (see [AccountHeroWidgetConfig]) — this config
 * screen is a simple account picker rather than reusing [WidgetConfigFiltersSection] alone,
 * since the single most important choice here is *which account*, not just the timeline. Not a
 * `@AndroidEntryPoint` activity like the rest of the widget package; it reaches Hilt bindings
 * through [WidgetDataLoaderEntryPoint] the same way every widget fetcher already does.
 */
@OptIn(ExperimentalMaterial3Api::class)
class AccountHeroWidgetConfigActivity : BaseWidgetConfigActivity<AccountHeroWidgetConfig>() {

    override val prefsStore by lazy { AccountHeroWidgetPrefsStore(applicationContext) }
    override val defaultConfig = AccountHeroWidgetConfig()

    @Composable
    override fun Content(
        config: AccountHeroWidgetConfig,
        onConfigChange: (AccountHeroWidgetConfig) -> Unit,
        onSave: () -> Unit,
        onBack: () -> Unit
    ) {
        AccountHeroWidgetConfigScreen(applicationContext, config, onConfigChange, onSave, onBack)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AccountHeroWidgetConfigScreen(
    appContext: android.content.Context,
    config: AccountHeroWidgetConfig,
    onConfigChange: (AccountHeroWidgetConfig) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    var accounts by remember { mutableStateOf<List<Account>>(emptyList()) }

    LaunchedEffect(Unit) {
        val ep = EntryPointAccessors.fromApplication(appContext, WidgetDataLoaderEntryPoint::class.java)
        val profileId = ep.settingsDataStore().currentProfileId.first()
        accounts = ep.accountRepository().getAllAccounts(profileId).first().filter { !it.isArchived }
    }

    WidgetConfigScreen(
        title = "Account Hero Widget",
        onBack = onBack,
        onSave = onSave
    ) {
        WidgetConfigSectionHeader("Pick an account")
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(accounts, key = { it.id }) { account ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onConfigChange(
                                config.copy(
                                    accountId = account.id,
                                    filter = config.filter.forAccount(account.id)
                                )
                            )
                        }
                        .padding(vertical = AeonTheme.dimensions.spacingSm)
                ) {
                    RadioButton(
                        selected = config.accountId == account.id,
                        onClick = {
                            onConfigChange(
                                config.copy(
                                    accountId = account.id,
                                    filter = config.filter.forAccount(account.id)
                                )
                            )
                        }
                    )
                    Text(text = account.name, color = AppColors.onSurface)
                }
            }
        }
    }
}
