package com.osscabinet.gleemanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.*
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
import android.graphics.Color as AndroidColor

@Composable
fun AppDrawerContent(
    currentProfile: ProfileEntity?,
    nonArchivedAccounts: List<Account>,
    selectedDestination: String,
    onDashboardClick: () -> Unit,
    onAccountsViewClick: () -> Unit,
    onRecordsClick: () -> Unit,
    onAnalyticsClick: () -> Unit,
    onBudgetsClick: () -> Unit,
    onLoansClick: () -> Unit,
    onManageAccountsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAccountClick: (String) -> Unit,
    onCloseDrawer: () -> Unit
) {
    ModalDrawerSheet(
        modifier = Modifier.width(320.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerShape = RoundedCornerShape(topEnd = 32.dp, bottomEnd = 32.dp)
    ) {
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
                "Personal Ledger",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        
        Spacer(modifier = Modifier.height(12.dp))

        NavigationDrawerItem(
            label = { Text("DASHBOARD", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)) },
            selected = selectedDestination == "dashboard",
            onClick = { 
                onDashboardClick()
                onCloseDrawer()
            },
            icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
            modifier = Modifier.padding(horizontal = 12.dp),
            colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
        )
        NavigationDrawerItem(
            label = { Text("ACCOUNTS", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)) },
            selected = selectedDestination == "accounts_view",
            onClick = { 
                onAccountsViewClick()
                onCloseDrawer()
            },
            icon = { Icon(Icons.Default.Wallet, contentDescription = null) },
            modifier = Modifier.padding(horizontal = 12.dp),
            colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
        )
        NavigationDrawerItem(
            label = { Text("RECORDS", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)) },
            selected = selectedDestination == "records",
            onClick = { 
                onRecordsClick()
                onCloseDrawer()
            },
            icon = { Icon(Icons.AutoMirrored.Filled.ListAlt, contentDescription = null) },
            modifier = Modifier.padding(horizontal = 12.dp),
            colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
        )
        NavigationDrawerItem(
            label = { Text("ANALYTICS", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)) },
            selected = selectedDestination == "analytics",
            onClick = { 
                onAnalyticsClick()
                onCloseDrawer()
            },
            icon = { Icon(Icons.Default.AutoGraph, contentDescription = null) },
            modifier = Modifier.padding(horizontal = 12.dp),
            colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
        )
        NavigationDrawerItem(
            label = { Text("BUDGETS", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)) },
            selected = selectedDestination == "budgets",
            onClick = { 
                onBudgetsClick()
                onCloseDrawer()
            },
            icon = { Icon(Icons.Default.PieChart, contentDescription = null) },
            modifier = Modifier.padding(horizontal = 12.dp),
            colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
        )
        NavigationDrawerItem(
            label = { Text("LOANS", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)) },
            selected = selectedDestination == "loans",
            onClick = { 
                onLoansClick()
                onCloseDrawer()
            },
            icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null) },
            modifier = Modifier.padding(horizontal = 12.dp),
            colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
        )

        HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        
        Text(
            "LEDGERS",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
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
                    val accountColor = try { Color(AndroidColor.parseColor(account.colorHex)) } catch (_: Exception) { MaterialTheme.colorScheme.primary }
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

        Spacer(modifier = Modifier.weight(1f))
        
        NavigationDrawerItem(
            label = { Text("SETTINGS", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)) },
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
