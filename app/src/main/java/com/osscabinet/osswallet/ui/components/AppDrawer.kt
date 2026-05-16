package com.osscabinet.ossWallet.ui.components

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
import androidx.compose.material.icons.rounded.EventRepeat
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
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Template
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.Profile
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import android.graphics.Color as AndroidColor
import com.osscabinet.ossWallet.ui.theme.AppColors

@Composable
fun AppDrawerContent(
    currentProfile: Profile?,
    nonArchivedAccounts: List<Account>,
    starredTemplates: List<Template>,
    selectedDestination: String,
    onDashboardClick: () -> Unit,
    onRecordsClick: () -> Unit,
    onAnalyticsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onExchangeRatesClick: () -> Unit,
    onAccountClick: (String) -> Unit,
    onTemplateClick: (Template) -> Unit,
    onCloseDrawer: () -> Unit,
    onPlannedPaymentsClick: () -> Unit = {},
    onBudgetsClick: () -> Unit = {},
    onLoansClick: () -> Unit = {}
) {
    ModalDrawerSheet(
        modifier = Modifier.width(280.dp),
        drawerContainerColor = AppColors.drawerSurface,
        drawerShape = RoundedCornerShape(topEnd = AeonTheme.dimensions.cardCornerRadius, bottomEnd = AeonTheme.dimensions.cardCornerRadius)
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
                        .padding(AeonTheme.dimensions.screenPadding)
                ) {
                    Surface(
                        modifier = Modifier.size(AeonTheme.dimensions.iconSize2xl),
                        shape = CircleShape,
                        color = AppColors.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(AeonTheme.dimensions.iconSizeMd),
                                tint = AppColors.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(AeonTheme.dimensions.spacingMd))
                    Text(
                        currentProfile?.name ?: "Intelligence",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        "Personal Account",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.contentSecondary
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding),
                    color = AppColors.insetSurface.copy(alpha = 0.5f)
                )

                Spacer(modifier = Modifier.height(AeonTheme.dimensions.spacingSm))

                // Navigation Items
                val navItems = listOf(
                    Triple("DASHBOARD", "dashboard", Icons.Default.Dashboard),
                    Triple("RECORDS", "list", Icons.AutoMirrored.Filled.ListAlt),
                    Triple("ANALYTICS", "analytics", Icons.Default.AutoGraph),
                    Triple("EXCHANGE RATES", "exchange_rates", Icons.Default.CurrencyExchange)
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
                                "list" -> onRecordsClick()
                                "analytics" -> onAnalyticsClick()
                                "exchange_rates" -> onExchangeRatesClick()
                            }
                            onCloseDrawer()
                        },
                        icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(AeonTheme.dimensions.iconSizeSm)) },
                        modifier = Modifier.padding(horizontal = AeonTheme.dimensions.spacingSm),
                        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = AppColors.transparent)
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding, vertical = AeonTheme.dimensions.spacingSm),
                    color = AppColors.insetSurface.copy(alpha = 0.5f)
                )

                Text(
                    "PLANNING",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = AppColors.contentSecondary,
                    modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding, vertical = AeonTheme.dimensions.spacingSm)
                )

                val planningItems = listOf(
                    Triple("PLANNED PAYMENTS", "planned_payments", Icons.Rounded.EventRepeat),
                    Triple("BUDGETS", "budgets", Icons.Default.Savings),
                    Triple("LOANS", "loans", Icons.Default.AccountBalance)
                )

                planningItems.forEach { (label, route, icon) ->
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
                                "planned_payments" -> onPlannedPaymentsClick()
                                "budgets" -> onBudgetsClick()
                                "loans" -> onLoansClick()
                            }
                            onCloseDrawer()
                        },
                        icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(AeonTheme.dimensions.iconSizeSm)) },
                        modifier = Modifier.padding(horizontal = AeonTheme.dimensions.spacingSm),
                        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = AppColors.transparent)
                    )
                }

                if (starredTemplates.isNotEmpty()) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding, vertical = AeonTheme.dimensions.spacingSm),
                        color = AppColors.insetSurface.copy(alpha = 0.5f)
                    )

                    Text(
                        "QUICK ACCESS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = AppColors.contentSecondary,
                        modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding, vertical = AeonTheme.dimensions.spacingSm)
                    )

                    starredTemplates.forEach { template ->
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
                                        TransactionType.INCOME -> Icons.Rounded.ArrowUpward
                                        TransactionType.TRANSFER -> Icons.Rounded.SwapHoriz
                                        else -> Icons.Rounded.ArrowDownward
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(AeonTheme.dimensions.iconSizeXs),
                                    tint = AppColors.primary
                                )
                            },
                            modifier = Modifier.padding(horizontal = AeonTheme.dimensions.spacingSm),
                            colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = AppColors.transparent)
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding, vertical = AeonTheme.dimensions.spacingSm),
                    color = AppColors.insetSurface.copy(alpha = 0.5f)
                )

                Text(
                    "ACCOUNTS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = AppColors.contentSecondary,
                    modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding, vertical = AeonTheme.dimensions.spacingSm)
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
                                    color = AppColors.contentSecondary
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
                                AppColors.primary
                            }
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(accountColor)
                            )
                        },
                        modifier = Modifier.padding(horizontal = AeonTheme.dimensions.spacingSm),
                        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = AppColors.transparent)
                    )
                }
            }

            // Static Footer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.panelSurface)
            ) {
                HorizontalDivider(
                    color = AppColors.insetSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding)
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
                    icon = { Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(AeonTheme.dimensions.iconSizeSm)) },
                    modifier = Modifier.padding(horizontal = AeonTheme.dimensions.spacingSm, vertical = AeonTheme.dimensions.spacingSm),
                    colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = AppColors.transparent)
                )
            }
        }
    }
}
