package com.osscabinet.gleemanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.gleemanager.domain.model.Account
import com.osscabinet.gleemanager.data.local.entity.ProfileEntity
import com.osscabinet.gleemanager.data.local.entity.TemplateEntity
import android.graphics.Color as AndroidColor

@Composable
fun AppDrawerContent(
    currentProfile: ProfileEntity?,
    nonArchivedAccounts: List<Account>,
    starredTemplates: List<TemplateEntity>,
    selectedDestination: String,
    onDashboardClick: () -> Unit,
    onAccountsViewClick: () -> Unit,
    onRecordsClick: () -> Unit,
    onAnalyticsClick: () -> Unit,
    onBudgetsClick: () -> Unit,
    onLoansClick: () -> Unit,
    onManageAccountsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onExchangeRatesClick: () -> Unit,
    onTemplatesClick: () -> Unit,
    onAccountClick: (String) -> Unit,
    onTemplateClick: (TemplateEntity) -> Unit,
    onCloseDrawer: () -> Unit
) {
    ModalDrawerSheet(
        modifier = Modifier.width(320.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerShape = RoundedCornerShape(topEnd = 32.dp, bottomEnd = 32.dp)
    ) {
        val scrollState = rememberScrollState()
        
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
            ) {
                // Profile Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(64.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        currentProfile?.name ?: "Intelligence",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        "Personal Account",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Navigation Items
                val navItems = listOf(
                    Triple("DASHBOARD", "dashboard", Icons.Default.Dashboard),
                    Triple("ACCOUNTS", "accounts_view", Icons.Default.Wallet),
                    Triple("RECORDS", "records", Icons.AutoMirrored.Filled.ListAlt),
                    Triple("ANALYTICS", "analytics", Icons.Default.AutoGraph),
                    Triple("BUDGETS", "budgets", Icons.Default.PieChart),
                    Triple("LOANS", "loans", Icons.Default.AccountBalanceWallet),
                    Triple("EXCHANGE RATES", "exchange_rates", Icons.Default.CurrencyExchange),
                    Triple("TEMPLATES", "templates", Icons.Default.AutoAwesome)
                )

                navItems.forEach { (label, route, icon) ->
                    NavigationDrawerItem(
                        label = {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        },
                        selected = selectedDestination == route,
                        onClick = {
                            when (route) {
                                "dashboard" -> onDashboardClick()
                                "accounts_view" -> onAccountsViewClick()
                                "records" -> onRecordsClick()
                                "analytics" -> onAnalyticsClick()
                                "budgets" -> onBudgetsClick()
                                "loans" -> onLoansClick()
                                "exchange_rates" -> onExchangeRatesClick()
                                "templates" -> onTemplatesClick()
                            }
                            onCloseDrawer()
                        },
                        icon = { Icon(icon, contentDescription = null) },
                        modifier = Modifier.padding(horizontal = 12.dp),
                        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                    )
                }

                if (starredTemplates.isNotEmpty()) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )

                    Text(
                        "QUICK ACCESS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )

                    starredTemplates.take(5).forEach { template ->
                        NavigationDrawerItem(
                            label = { Text(template.name, style = MaterialTheme.typography.labelLarge) },
                            selected = false,
                            onClick = {
                                onTemplateClick(template)
                                onCloseDrawer()
                            },
                            icon = {
                                Icon(
                                    when (template.transactionType) {
                                        1 -> Icons.Rounded.ArrowUpward
                                        2 -> Icons.Rounded.SwapHoriz
                                        else -> Icons.Rounded.ArrowDownward
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            modifier = Modifier.padding(horizontal = 12.dp),
                            colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )

                Text(
                    "ACCOUNTS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )

                nonArchivedAccounts.sortedBy { it.name }.forEach { account ->
                    NavigationDrawerItem(
                        label = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    account.name,
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    account.currencyCode,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        },
                        selected = false,
                        onClick = {
                            onAccountClick(account.id)
                            onCloseDrawer()
                        },
                        icon = {
                            val accountColor = try {
                                Color(AndroidColor.parseColor(account.colorHex))
                            } catch (_: Exception) {
                                MaterialTheme.colorScheme.primary
                            }
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(accountColor)
                            )
                        },
                        modifier = Modifier.padding(horizontal = 12.dp),
                        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                    )
                }
            }

            // Static Footer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                NavigationDrawerItem(
                    label = {
                        Text(
                            "SETTINGS",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    selected = selectedDestination == "settings",
                    onClick = {
                        onSettingsClick()
                        onCloseDrawer()
                    },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                    colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                )
            }
        }
    }
}
