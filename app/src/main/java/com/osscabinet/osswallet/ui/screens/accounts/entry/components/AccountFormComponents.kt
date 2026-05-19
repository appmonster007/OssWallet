package com.osscabinet.ossWallet.ui.screens.accounts.entry.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.ui.components.AeonFlowSectionHeader
import com.osscabinet.ossWallet.ui.util.isDark
import java.util.Currency.getAvailableCurrencies
import com.osscabinet.ossWallet.ui.theme.*
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.getCurrencySymbol
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.getCurrencyName

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CurrencyManagementSection(
    primaryCurrency: String,
    onPrimaryCurrencyChange: (String) -> Unit,
    allowedCurrencies: Set<String>,
    onAllowedCurrenciesChange: (Set<String>) -> Unit,
    accentColorHex: String,
    isEditMode: Boolean = false,
    currencyUsageMap: Map<String, Boolean> = emptyMap()
) {
    var searchQuery by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current
    val accentColor = try { Color(android.graphics.Color.parseColor(accentColorHex)) } catch (e: Exception) { AppColors.primary }
    
    val commonCurrencies = listOf("USD", "EUR", "GBP", "JPY", "CHF", "CAD", "AUD", "INR", "SGD", "HKD", "CNY", "BRL", "IDR", "MYR", "THB", "PHP", "VND", "KRW", "RUB", "TRY", "ZAR", "MXN", "ILS")
    
    val allCurrencies = remember {
        getAvailableCurrencies()
            .map { it.currencyCode }
            .distinct()
            .sorted()
    }
    
    val filteredCurrencies = if (searchQuery.isBlank()) {
        commonCurrencies
    } else {
        allCurrencies.filter { 
            it.contains(searchQuery, ignoreCase = true) || 
            try { java.util.Currency.getInstance(it).displayName.contains(searchQuery, ignoreCase = true) } catch (e: Exception) { false }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacing2xl)
    ) {
        // Search & Header
        Column(verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.cardPadding)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "ASSET CONFIGURATION",
                        style = AppTextStyles.tableColumnHeader,
                        color = AppColors.primary
                    )
                    Text(
                        "Select currencies for this portfolio",
                        style = AppTextStyles.caption,
                        color = AppColors.contentSecondary.copy(alpha = 0.5f)
                    )
                }
                
                Surface(
                    shape = RoundedCornerShape(AeonTheme.dimensions.spacingMd),
                    color = AppColors.primary,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = AeonTheme.dimensions.spacingMd, vertical = AeonSize.D6.value),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm)
                    ) {
                        Icon(Icons.Rounded.Dashboard, contentDescription = null, tint = Color.White, modifier = Modifier.size(AeonSize.D14.value))
                        Text(
                            allowedCurrencies.size.toString(),
                            style = AppTextStyles.cardDelta.copy(fontWeight = FontWeight.Black),
                            color = Color.White
                        )
                    }
                }
            }
            
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AeonTheme.dimensions.spacingMd),
                color = AppColors.recessedSurface.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, AppColors.divider.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = AeonTheme.dimensions.cardPadding, vertical = AeonTheme.dimensions.spacingMd),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Search, contentDescription = null, tint = AppColors.primary.copy(alpha = 0.5f), modifier = Modifier.size(AeonSize.D20.value))
                    Spacer(modifier = Modifier.width(AeonTheme.dimensions.spacingMd))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        textStyle = AppTextStyles.listItemTitle.copy(color = AppColors.contentPrimary),
                        cursorBrush = SolidColor(AppColors.primary),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) Text("Search assets...", style = AppTextStyles.listItemTitle, color = AppColors.contentSecondary.copy(alpha = 0.3f))
                            innerTextField()
                        }
                    )
                }
            }
        }

        // Active Set (Tiles)
        if (allowedCurrencies.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.cardPadding)) {
                Text(
                    "SELECTED PORTFOLIO",
                    style = AppTextStyles.sectionOverline,
                    color = AppColors.contentSecondary.copy(alpha = 0.5f)
                )
                
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
                ) {
                    allowedCurrencies.forEach { code ->
                        val isPrimary = primaryCurrency == code
                        CurrencyTile(
                            code = code,
                            isPrimary = isPrimary,
                            accentColor = accentColor,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                if (isPrimary) {
                                    // Toggle off primary if multiple, or just leave it
                                } else {
                                    onPrimaryCurrencyChange(code)
                                }
                            },
                            onRemove = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (isPrimary) onPrimaryCurrencyChange("")
                                onAllowedCurrenciesChange(allowedCurrencies - code)
                            }
                        )
                    }
                }
            }
        }

        // Discovery Grid
        Column(verticalArrangement = Arrangement.spacedBy(AeonSize.D20.value)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    if (searchQuery.isBlank()) "POPULAR ASSETS" else "SEARCH RESULTS",
                    style = AppTextStyles.sectionOverline,
                    color = AppColors.contentSecondary.copy(alpha = 0.5f)
                )
                if (searchQuery.isBlank()) {
                    Text(
                        "SEE ALL",
                        style = AppTextStyles.tagLabel.copy(fontWeight = FontWeight.Black, color = AppColors.primary)
                    )
                }
            }
            
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.chipPadding)
            ) {
                filteredCurrencies.filter { !allowedCurrencies.contains(it) }.forEach { code ->
                    Surface(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            if (allowedCurrencies.isEmpty()) onPrimaryCurrencyChange(code)
                            onAllowedCurrenciesChange(allowedCurrencies + code)
                        },
                        shape = RoundedCornerShape(AeonTheme.dimensions.cardCornerRadius),
                        color = AppColors.recessedSurface.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, AppColors.divider.copy(alpha = 0.1f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = AeonSize.D14.value, vertical = AeonTheme.dimensions.spacingMd),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm)
                        ) {
                            Text(
                                code,
                                style = AppTextStyles.drawerItemLabel.copy(fontWeight = FontWeight.Black),
                                color = AppColors.contentPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CurrencyTile(
    code: String,
    isPrimary: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.width(100.dp).height(70.dp),
        shape = RoundedCornerShape(AeonTheme.dimensions.buttonCornerRadius),
        color = if (isPrimary) accentColor else AppColors.recessedSurface.copy(alpha = 0.5f),
        border = BorderStroke(
            width = if (isPrimary) 2.dp else 1.dp,
            color = if (isPrimary) accentColor else AppColors.divider.copy(alpha = 0.1f)
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!isPrimary) {
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.align(Alignment.TopEnd).size(AeonTheme.dimensions.buttonCornerRadius).padding(AeonSize.D4.value)
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Remove", modifier = Modifier.size(AeonTheme.dimensions.iconSizeXs), tint = AppColors.contentSecondary.copy(alpha = 0.4f))
                }
            } else {
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(AeonTheme.dimensions.spacingSm),
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.2f)
                ) {
                    Icon(
                        Icons.Rounded.Star, 
                        contentDescription = "Primary", 
                        modifier = Modifier.padding(AeonSize.D4.value).size(AeonSize.D10.value),
                        tint = Color.White
                    )
                }
            }
            
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    getCurrencySymbol(code),
                    style = AppTextStyles.heroAmount.copy(fontWeight = FontWeight.Black),
                    color = if (isPrimary) Color.White else AppColors.contentPrimary
                )
                Text(
                    code,
                    style = AppTextStyles.overlineLabel.copy(fontWeight = FontWeight.Black),
                    color = if (isPrimary) Color.White.copy(alpha = 0.7f) else AppColors.contentSecondary
                )
            }
        }
    }
}
