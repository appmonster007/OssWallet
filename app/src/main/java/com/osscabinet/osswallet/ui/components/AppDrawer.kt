package com.osscabinet.ossWallet.ui.components

import com.osscabinet.ossWallet.domain.model.EntryMode
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.remember
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.DrawerDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Profile
import com.osscabinet.ossWallet.domain.model.Template
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.GlassTokens
import com.osscabinet.ossWallet.ui.theme.scaledDp
import android.graphics.Color as AndroidColor
import dev.chrisbanes.haze.HazeState

@Composable
fun AppDrawerContent(
    drawerHazeState: HazeState,
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
    onPlannedPaymentsClick: () -> Unit = {},
    onBudgetsClick: () -> Unit = {},
    onLoansClick: () -> Unit = {},
    // Caller closes the drawer first, then invokes the action — eliminates the
    // navigate-while-animating race that caused inconsistent tap behaviour.
    onNavigate: (() -> Unit) -> Unit
) {
    val uiScale = AeonTheme.uiScale
    val drawerShape = RoundedCornerShape(
        topEnd = AeonTheme.dimensions.cardCornerRadius,
        bottomEnd = AeonTheme.dimensions.cardCornerRadius
    )
    val scrollState = rememberScrollState()

    // H editorial: accent-tinted selected state, transparent unselected
    val drawerItemColors = NavigationDrawerItemDefaults.colors(
        selectedContainerColor = AppColors.accent.copy(alpha = 0.10f),
        unselectedContainerColor = Color.Transparent,
        selectedIconColor = AppColors.accent,
        unselectedIconColor = AppColors.contentSecondary,
        selectedTextColor = AppColors.contentPrimary,
        unselectedTextColor = AppColors.contentPrimary
    )

    GlassSurface(
        modifier = Modifier
            .width(scaledDp(280f, uiScale))
            .fillMaxHeight(),
        shape = drawerShape,
        hazeState = drawerHazeState,
        hazeStyle = GlassTokens.drawerStyle(),
        glassVariant = com.osscabinet.ossWallet.ui.theme.GlassVariant.Drawer,
        fallbackColor = AppColors.drawerSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(DrawerDefaults.windowInsets)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
            ) {
                // ── Profile header ───────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate { onSettingsClick() } }
                        .padding(
                            horizontal = AeonTheme.dimensions.listPadding,
                            vertical = AeonTheme.dimensions.screenPadding
                        )
                ) {
                    // H editorial: inset square with initial letter
                    val initial = (currentProfile?.name?.firstOrNull() ?: '?').uppercaseChar().toString()
                    val avatarShape = RoundedCornerShape(10.dp)
                    Box(
                        modifier = Modifier
                            .size(AeonTheme.dimensions.iconSize2xl)
                            .clip(avatarShape)
                            .background(AppColors.screenBackground)
                            .border(0.5.dp, AppColors.outlineVariant, avatarShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initial,
                            style = AppTextStyles.sectionOverline,
                            color = AppColors.accent
                        )
                    }
                    Spacer(modifier = Modifier.height(AeonTheme.dimensions.spacingMd))
                    Text(
                        currentProfile?.name ?: "Intelligence",
                        style = AppTextStyles.heroAmount,
                        color = AppColors.contentPrimary
                    )
                    Text(
                        "PERSONAL ACCOUNT",
                        style = AppTextStyles.metaLabel,
                        color = AppColors.contentSecondary
                    )
                }

                DrawerHairline()
                Spacer(modifier = Modifier.height(AeonTheme.dimensions.spacingSm))

                // ── Primary navigation ───────────────────────────────────
                val navItems = listOf(
                    Triple("DASHBOARD", "dashboard", Icons.Default.Dashboard),
                    Triple("RECORDS", "transaction_list", Icons.AutoMirrored.Filled.ListAlt),
                    Triple("ANALYTICS", "analytics", Icons.Default.AutoGraph),
                    Triple("EXCHANGE RATES", "exchange_rates", Icons.Default.CurrencyExchange)
                )
                navItems.forEach { (label, route, icon) ->
                    NavigationDrawerItem(
                        label = { Text(label, style = AppTextStyles.listSectionHeader) },
                        selected = selectedDestination == route,
                        onClick = {
                            onNavigate {
                                when (route) {
                                    "dashboard"      -> onDashboardClick()
                                    "transaction_list" -> onRecordsClick()
                                    "analytics"      -> onAnalyticsClick()
                                    "exchange_rates" -> onExchangeRatesClick()
                                }
                            }
                        },
                        icon = {
                            Icon(
                                icon,
                                contentDescription = null,
                                modifier = Modifier.size(AeonTheme.dimensions.iconSizeSm)
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RectangleShape,
                        colors = drawerItemColors
                    )
                }

                // ── PLANNING ─────────────────────────────────────────────
                DrawerSectionLabel("PLANNING")

                val planningItems = listOf(
                    Triple("PLANNED PAYMENTS", "planned_payment_list", Icons.Rounded.EventRepeat),
                    Triple("BUDGETS", "budget_list", Icons.Default.Savings),
                    Triple("LOANS", "loan_list", Icons.Default.AccountBalance)
                )
                planningItems.forEach { (label, route, icon) ->
                    NavigationDrawerItem(
                        label = { Text(label, style = AppTextStyles.listSectionHeader) },
                        selected = selectedDestination == route,
                        onClick = {
                            onNavigate {
                                when (route) {
                                    "planned_payment_list" -> onPlannedPaymentsClick()
                                    "budget_list"          -> onBudgetsClick()
                                    "loan_list"            -> onLoansClick()
                                }
                            }
                        },
                        icon = {
                            Icon(
                                icon,
                                contentDescription = null,
                                modifier = Modifier.size(AeonTheme.dimensions.iconSizeSm)
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RectangleShape,
                        colors = drawerItemColors
                    )
                }

                // ── QUICK ACCESS (starred templates) ─────────────────────
                if (starredTemplates.isNotEmpty()) {
                    DrawerSectionLabel("QUICK ACCESS")

                    starredTemplates.forEach { template ->
                        NavigationDrawerItem(
                            label = {
                                Text(
                                    template.name,
                                    style = AppTextStyles.drawerItemLabel,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            selected = false,
                            onClick = { onNavigate { onTemplateClick(template) } },
                            icon = {
                                Icon(
                                    when (template.entryMode) {
                                        EntryMode.INCOME   -> Icons.Rounded.ArrowUpward
                                        EntryMode.TRANSFER -> Icons.Rounded.SwapHoriz
                                        else               -> Icons.Rounded.ArrowDownward
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(AeonTheme.dimensions.iconSizeXs),
                                    tint = AppColors.accent
                                )
                            },
                            modifier = Modifier.fillMaxWidth().height(scaledDp(48f, uiScale)),
                            shape = RectangleShape,
                            colors = drawerItemColors
                        )
                    }
                }

                // ── ACCOUNTS ─────────────────────────────────────────────
                DrawerSectionLabel("ACCOUNTS")

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
                                    style = AppTextStyles.drawerItemLabel,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    account.currencyCode,
                                    style = AppTextStyles.metaLabel,
                                    color = AppColors.contentSecondary
                                )
                            }
                        },
                        selected = false,
                        onClick = { onNavigate { onAccountClick(account.id) } },
                        icon = {
                            val accountColor = try {
                                Color(AndroidColor.parseColor(account.colorHex))
                            } catch (_: Exception) {
                                AppColors.accent
                            }
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(accountColor)
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RectangleShape,
                        colors = drawerItemColors
                    )
                }
            }

            // ── Static footer — Settings ──────────────────────────────
            Column(modifier = Modifier.fillMaxWidth()) {
                DrawerHairline()
                NavigationDrawerItem(
                    label = { Text("SETTINGS", style = AppTextStyles.listSectionHeader) },
                    selected = selectedDestination == "settings",
                    onClick = { onNavigate { onSettingsClick() } },
                    icon = {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(AeonTheme.dimensions.iconSizeSm)
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RectangleShape,
                    colors = drawerItemColors
                )
            }
        }
    }
}

/** Mono section overline with a hairline rule to the right — H editorial separator. */
@Composable
private fun DrawerSectionLabel(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = AeonTheme.dimensions.listPadding,
                end = AeonTheme.dimensions.listPadding,
                top = AeonTheme.dimensions.spacingMd,
                bottom = AeonTheme.dimensions.spacingXs
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm)
    ) {
        Text(title, style = AppTextStyles.sectionOverline, color = AppColors.accent)
        Box(
            modifier = Modifier
                .weight(1f)
                .height(0.5.dp)
                .background(AppColors.outlineVariant)
        )
    }
}

/** 0.5dp hairline — replaces HorizontalDivider in the drawer. */
@Composable
private fun DrawerHairline() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AeonTheme.dimensions.listPadding)
            .height(0.5.dp)
            .background(AppColors.outlineVariant)
    )
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun AppDrawerContentPreview() {
    val mockProfile = Profile(1, "OssWallet User", "USD")
    val mockAccounts = listOf(
        Account(
            "1",
            "Main Savings",
            "USD",
            com.osscabinet.ossWallet.domain.model.AccountStructureType.SAVINGS,
            "#004D40",
            "AccountBalance",
            emptyList()
        ),
        Account(
            "2",
            "Travel Fund",
            "EUR",
            com.osscabinet.ossWallet.domain.model.AccountStructureType.SAVINGS,
            "#B3261E",
            "Travel",
            emptyList()
        )
    )

    com.osscabinet.ossWallet.ui.theme.OssWalletTheme {
        AppDrawerContent(
            drawerHazeState = remember { dev.chrisbanes.haze.HazeState() },
            currentProfile = mockProfile,
            nonArchivedAccounts = mockAccounts,
            starredTemplates = emptyList(),
            selectedDestination = "dashboard",
            onDashboardClick = {},
            onRecordsClick = {},
            onAnalyticsClick = {},
            onSettingsClick = {},
            onExchangeRatesClick = {},
            onAccountClick = {},
            onTemplateClick = {},
            onNavigate = { it() }
        )
    }
}
