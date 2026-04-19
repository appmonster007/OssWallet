package com.osscabinet.ossWallet.ui.widget.config

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.model.settings.NetWorthSettings
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.data.local.entity.ProfileEntity
import com.osscabinet.ossWallet.ui.components.TimelineFilterBar
import com.osscabinet.ossWallet.ui.components.AeonSectionHeader
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class NetWorthWidgetConfigActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    companion object {
        val KEY_SETTINGS = stringPreferencesKey("net_worth_settings")
        val KEY_TIMELINE = stringPreferencesKey("timeline_range")
        val KEY_FONT_SIZE = stringPreferencesKey("font_size")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set the result to CANCELED. This will cause the widget host to cancel
        // out of the widget placement if the user presses the back button.
        setResult(RESULT_CANCELED)

        // Find the widget id from the intent.
        val extras = intent.extras
        if (extras != null) {
            appWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }

        // If this activity was started with an invalid widget ID, finish with an error.
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            val container = (applicationContext as com.osscabinet.ossWallet.OssWalletApp).container
            val profileRepository = container.profileRepository
            val accountRepository = container.accountRepository
            val settingsDataStore = container.settingsDataStore

            val profiles by profileRepository.getAllProfiles().collectAsState(initial = emptyList())
            val currentProfileId by settingsDataStore.currentProfileId.collectAsState(initial = 1)
            
            var selectedProfileId by remember(currentProfileId) { mutableIntStateOf(currentProfileId) }
            val accounts by accountRepository.getAllAccounts(selectedProfileId).collectAsState(initial = emptyList())

            OssWalletTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ConfigScreen(
                        profiles = profiles,
                        selectedProfileId = selectedProfileId,
                        onProfileSelected = { selectedProfileId = it },
                        accounts = accounts,
                        onConfirm = { settings ->
                            saveAndFinish(settings)
                        }
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun ConfigScreen(
        profiles: List<ProfileEntity>,
        selectedProfileId: Int,
        onProfileSelected: (Int) -> Unit,
        accounts: List<Account>,
        onConfirm: (NetWorthSettings) -> Unit
    ) {
        var timelineRange by remember { mutableStateOf<TimelineRange>(TimelineRange.AllTime) }
        var fontSize by remember { mutableStateOf("Medium") }
        var selectedAccountIds by remember { mutableStateOf<Set<String>?>(null) }

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Widget Settings", fontWeight = FontWeight.Bold) }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    "Configure your Net Worth widget to display exactly what you need.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Profile Section
                SectionCard(title = "PROFILE") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        profiles.forEach { profile ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onProfileSelected(profile.id) }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedProfileId == profile.id,
                                    onClick = { onProfileSelected(profile.id) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(profile.name)
                            }
                        }
                    }
                }

                // Timeline Section
                SectionCard(title = "TIMELINE") {
                    TimelineFilterBar(
                        currentRange = timelineRange,
                        onRangeSelected = { timelineRange = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Accounts Section
                SectionCard(title = "ACCOUNTS") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedAccountIds = null }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selectedAccountIds == null,
                                onCheckedChange = { if (it) selectedAccountIds = null }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("All Accounts", fontWeight = FontWeight.Bold)
                        }
                        
                        accounts.forEach { account ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val current = selectedAccountIds ?: emptySet()
                                        selectedAccountIds = if (account.id in current) {
                                            val next = current - account.id
                                            if (next.isEmpty()) null else next
                                        } else {
                                            current + account.id
                                        }
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = selectedAccountIds?.contains(account.id) == true,
                                    onCheckedChange = {
                                        val current = selectedAccountIds ?: emptySet()
                                        selectedAccountIds = if (it) {
                                            current + account.id
                                        } else {
                                            val next = current - account.id
                                            if (next.isEmpty()) null else next
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(account.name)
                            }
                        }
                    }
                }

                // Font Size Section
                SectionCard(title = "FONT SIZE") {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        listOf("Small", "Medium", "Large", "X-Large").forEachIndexed { index, size ->
                            SegmentedButton(
                                selected = fontSize == size,
                                onClick = { fontSize = size },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = 4),
                                label = { 
                                    Text(
                                        when(size) {
                                            "Small" -> "S"
                                            "Medium" -> "M"
                                            "Large" -> "L"
                                            else -> "XL"
                                        }
                                    )
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = { 
                        onConfirm(
                            NetWorthSettings(
                                fontSize = fontSize,
                                timelineRange = timelineRange,
                                profileId = selectedProfileId,
                                selectedAccountIds = selectedAccountIds?.toList()
                            )
                        ) 
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Text("CREATE WIDGET", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    @Composable
    private fun SectionCard(title: String, content: @Composable () -> Unit) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    content()
                }
            }
        }
    }

    private fun saveAndFinish(settings: NetWorthSettings) {
        lifecycleScope.launch {
            val context = this@NetWorthWidgetConfigActivity
            val glanceId = GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId)
            
            updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                prefs.toMutablePreferences().apply {
                    this[KEY_SETTINGS] = Json.encodeToString(settings)
                    // Legacy support
                    this[KEY_TIMELINE] = Json.encodeToString(settings.timelineRange)
                    this[KEY_FONT_SIZE] = settings.fontSize
                }
            }
            
            com.osscabinet.ossWallet.ui.widget.NetWorthWidget().update(context, glanceId)

            val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            setResult(RESULT_OK, resultValue)
            finish()
        }
    }
}
