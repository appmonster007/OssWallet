package com.osscabinet.ossWallet.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Deselect
import androidx.compose.material.icons.rounded.Flip
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.ui.components.AeonHazeProvider
import com.osscabinet.ossWallet.ui.components.AeonTopAppBar
import com.osscabinet.ossWallet.ui.components.AeonTopBarActionPill
import com.osscabinet.ossWallet.ui.components.AeonTopBarNavTitlePill
import com.osscabinet.ossWallet.ui.components.AeonTopBarSection
import com.osscabinet.ossWallet.ui.components.AppBarIconButton
import com.osscabinet.ossWallet.ui.components.aeonHazeSource
import com.osscabinet.ossWallet.ui.theme.AeonSize
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.HazeLayer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortlistedCurrenciesScreen(
    viewModel: SettingsViewModel,
    actions: ShortlistedCurrenciesActions
) {
    val availableCurrencies by viewModel.availableCurrencies.collectAsStateWithLifecycle()
    val shortlistedCurrencies by viewModel.shortlistedCurrencies.collectAsStateWithLifecycle()

    val selected = shortlistedCurrencies
        ?: availableCurrencies.map { it.code }.toSet()

    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(searchQuery, availableCurrencies) {
        availableCurrencies.filter {
            it.code.contains(searchQuery, ignoreCase = true) ||
                    it.name.contains(searchQuery, ignoreCase = true)
        }
    }
    val filteredCodes = remember(filtered) { filtered.map { it.code }.toSet() }
    val haptic = LocalHapticFeedback.current

    AeonHazeProvider(backgroundColor = AppColors.screenBackground) { hazeLayers ->
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0)
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .aeonHazeSource(hazeLayers.states[HazeLayer.Overlay])
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .padding(top = AeonSize.D64.value)
                            .padding(horizontal = 24.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            placeholder = { Text("Search currencies...") },
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Rounded.Clear, contentDescription = "Clear")
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                unfocusedContainerColor = AppColors.cardSurface,
                                focusedContainerColor = AppColors.cardSurface,
                                unfocusedIndicatorColor = AppColors.divider.copy(alpha = 0.1f),
                                focusedIndicatorColor = AppColors.primary.copy(alpha = 0.5f)
                            )
                        )

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = AppColors.cardSurface,
                            border = BorderStroke(0.5.dp, AppColors.divider.copy(alpha = 0.15f))
                        ) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filtered, key = { it.code }) { metadata ->
                                    val isSelected = metadata.code in selected
                                    Surface(
                                        onClick = {
                                            val next =
                                                if (isSelected) selected - metadata.code else selected + metadata.code
                                            if (next.isNotEmpty()) viewModel.setShortlistedCurrencies(
                                                next
                                            )
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) AppColors.primary.copy(alpha = 0.1f) else AppColors.transparent,
                                        border = if (isSelected) BorderStroke(
                                            0.5.dp,
                                            AppColors.primary.copy(alpha = 0.3f)
                                        ) else null
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                modifier = Modifier.size(40.dp),
                                                shape = CircleShape,
                                                color = if (isSelected) AppColors.primary.copy(alpha = 0.2f) else AppColors.recessedSurface
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        CurrencyService.getCurrencySymbol(metadata.code),
                                                        style = AppTextStyles.cardSectionTitle,
                                                        color = if (isSelected) AppColors.primary else AppColors.contentSecondary
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    metadata.name,
                                                    style = AppTextStyles.listItemTitle.copy(
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                                                    )
                                                )
                                                Text(
                                                    "${metadata.code} • ${metadata.country}",
                                                    style = AppTextStyles.transactionSwipeAction,
                                                    color = AppColors.contentSecondary.copy(alpha = 0.6f)
                                                )
                                            }
                                            if (isSelected) {
                                                Icon(
                                                    Icons.Rounded.CheckCircle,
                                                    contentDescription = "Selected",
                                                    tint = AppColors.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            AeonTopAppBar(
                left = AeonTopBarSection(
                    content = {
                        AeonTopBarNavTitlePill(
                            title = "Active Currencies",
                            subtitle = "${selected.size} selected",
                            onBackClick = actions.onBack
                        )
                    }
                ),
                right = AeonTopBarSection(
                    visible = filteredCodes.isNotEmpty(),
                    content = {
                        AeonTopBarActionPill {
                            AppBarIconButton(icon = Icons.Rounded.SelectAll, onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.setShortlistedCurrencies(selected + filteredCodes)
                            })
                            AppBarIconButton(icon = Icons.Rounded.Deselect, onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.setShortlistedCurrencies(selected - filteredCodes)
                            })
                            AppBarIconButton(icon = Icons.Rounded.Flip, onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val toggled = selected.toMutableSet()
                                filteredCodes.forEach { code ->
                                    if (code in toggled) toggled.remove(code) else toggled.add(code)
                                }
                                viewModel.setShortlistedCurrencies(toggled)
                            })
                        }
                    }
                )
            )
        }
    }
}

data class ShortlistedCurrenciesActions(
    val onBack: () -> Unit
)
