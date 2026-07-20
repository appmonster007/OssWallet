package com.osscabinet.ossWallet.ui.screens.accounts.entry.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.ui.theme.AeonSize
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import java.util.Currency.getAvailableCurrencies

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CurrencyManagementSection(
    primaryCurrency: String,
    onPrimaryCurrencyChange: (String) -> Unit,
    allowedCurrencies: Set<String>,
    onAllowedCurrenciesChange: (Set<String>) -> Unit,
    accentColorHex: String,
    isEditMode: Boolean = false,
    currencyUsageMap: Map<String, Boolean> = emptyMap(),
    shortlistedCurrencies: List<String> = emptyList()
) {
    var searchQuery by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current
    val accentColor = try {
        Color(android.graphics.Color.parseColor(accentColorHex))
    } catch (e: Exception) {
        AppColors.primary
    }

    val commonCurrencies = listOf(
        "USD",
        "EUR",
        "GBP",
        "JPY",
        "CHF",
        "CAD",
        "AUD",
        "INR",
        "SGD",
        "HKD",
        "CNY",
        "BRL",
        "IDR",
        "MYR",
        "THB",
        "PHP",
        "VND",
        "KRW",
        "RUB",
        "TRY",
        "ZAR",
        "MXN",
        "ILS"
    )

    val allCurrencies = remember {
        getAvailableCurrencies()
            .map { it.currencyCode }
            .distinct()
            .sorted()
    }

    val filteredCurrencies = if (searchQuery.isBlank()) {
        shortlistedCurrencies.ifEmpty { commonCurrencies }
    } else {
        allCurrencies.filter {
            it.contains(searchQuery, ignoreCase = true) ||
                    try {
                        java.util.Currency.getInstance(it).displayName.contains(
                            searchQuery,
                            ignoreCase = true
                        )
                    } catch (e: Exception) {
                        false
                    }
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
                        color = AppColors.contentSecondary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(AeonTheme.dimensions.spacingMd),
                    color = AppColors.primary,
                    border = BorderStroke(1.dp, AppColors.outline)
                ) {
                    Row(
                        modifier = Modifier.padding(
                            horizontal = AeonTheme.dimensions.spacingMd,
                            vertical = AeonSize.D6.value
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm)
                    ) {
                        Icon(
                            Icons.Rounded.Dashboard,
                            contentDescription = null,
                            tint = AppColors.onPrimary,
                            modifier = Modifier.size(AeonSize.D14.value)
                        )
                        Text(
                            allowedCurrencies.size.toString(),
                            style = AppTextStyles.cardDelta.copy(fontWeight = FontWeight.Black),
                            color = AppColors.onPrimary
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AeonTheme.dimensions.spacingMd),
                color = AppColors.surfaceContainerLow,
                border = BorderStroke(1.dp, AppColors.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = AeonTheme.dimensions.cardPadding,
                        vertical = AeonTheme.dimensions.spacingMd
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.Search,
                        contentDescription = null,
                        tint = AppColors.primary,
                        modifier = Modifier.size(AeonSize.D20.value)
                    )
                    Spacer(modifier = Modifier.width(AeonTheme.dimensions.spacingMd))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        textStyle = AppTextStyles.formFieldValue,
                        cursorBrush = SolidColor(AppColors.primary),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) Text(
                                "Search assets...",
                                style = AppTextStyles.formFieldHint,
                                color = AppColors.contentSecondary
                            )
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
                    color = AppColors.contentSecondary
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
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
                    // Phantom items to ensure left-alignment in the last row
                    repeat(10) {
                        Spacer(Modifier.width(100.dp).height(0.dp))
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
                    color = AppColors.contentSecondary
                )
                if (searchQuery.isBlank()) {
                    Text(
                        "SEE ALL",
                        style = AppTextStyles.chipLabelBold,
                        color = AppColors.primary
                    )
                }
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
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
                        color = AppColors.surfaceContainerLow,
                        border = BorderStroke(1.dp, AppColors.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                horizontal = AeonSize.D14.value,
                                vertical = AeonTheme.dimensions.spacingMd
                            ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm)
                        ) {
                            Text(
                                code,
                                style = AppTextStyles.managementItemTitle,
                                color = AppColors.contentPrimary
                            )
                        }
                    }
                }
                // Phantom items to ensure left-alignment in the last row
                repeat(10) {
                    Spacer(Modifier.width(80.dp).height(0.dp))
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
        modifier = Modifier
            .width(100.dp)
            .height(70.dp),
        shape = RoundedCornerShape(AeonTheme.dimensions.buttonCornerRadius),
        color = if (isPrimary) accentColor else AppColors.surfaceContainerLow,
        border = BorderStroke(
            width = if (isPrimary) 2.dp else 1.dp,
            color = if (isPrimary) accentColor else AppColors.outlineVariant
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!isPrimary) {
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(AeonTheme.dimensions.buttonCornerRadius)
                        .padding(AeonSize.D4.value)
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Remove",
                        modifier = Modifier.size(AeonTheme.dimensions.iconSizeXs),
                        tint = AppColors.contentSecondary
                    )
                }
            } else {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(AeonTheme.dimensions.spacingSm),
                    shape = CircleShape,
                    color = AppColors.onPrimary.copy(alpha = 0.2f)
                ) {
                    Icon(
                        Icons.Rounded.Star,
                        contentDescription = "Primary",
                        modifier = Modifier
                            .padding(AeonSize.D4.value)
                            .size(AeonSize.D10.value),
                        tint = AppColors.onPrimary
                    )
                }
            }

            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    CurrencyService.getCurrencySymbol(code),
                    style = AppTextStyles.heroAmount,
                    color = if (isPrimary) AppColors.onPrimary else AppColors.contentPrimary
                )
                Text(
                    code,
                    style = AppTextStyles.overlineLabel,
                    color = if (isPrimary) AppColors.onPrimary else AppColors.contentSecondary
                )
            }
        }
    }
}

