package com.osscabinet.ossWallet.ui.screens.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Api
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Deselect
import androidx.compose.material.icons.rounded.Flip
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.SettingsSuggest
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.formatAuto
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.getCurrencySymbol
import com.osscabinet.ossWallet.domain.service.currency.provider.CurrencyMetadata
import com.osscabinet.ossWallet.domain.service.currency.provider.ExchangeRateProvider
import com.osscabinet.ossWallet.ui.components.AeonHazeProvider
import com.osscabinet.ossWallet.ui.components.AeonTopAppBar
import com.osscabinet.ossWallet.ui.components.AeonTopBarNavTitlePill
import com.osscabinet.ossWallet.ui.components.AeonTopBarSection
import com.osscabinet.ossWallet.ui.components.ContourBackground
import com.osscabinet.ossWallet.ui.components.entry.EntryEditorialInput
import com.osscabinet.ossWallet.ui.components.scrollBottomPadding
import com.osscabinet.ossWallet.ui.theme.AeonSize
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.HazeLayer
import dev.chrisbanes.haze.hazeSource
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import kotlinx.coroutines.launch
import java.math.BigDecimal

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    actions: OnboardingActions
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.onboardingCompleted.collect {
            actions.onComplete()
        }
    }

    OnboardingContent(
        uiState = uiState,
        onEvent = viewModel::onEvent
    )
}

data class OnboardingActions(
    val onComplete: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingContent(
    uiState: OnboardingState,
    onEvent: (OnboardingEvent) -> Unit,
) {
    val totalSteps = if (uiState.showDebugSeedStep) 6 else 5
    val pagerState = rememberPagerState { totalSteps }
    val scope = rememberCoroutineScope()

    AeonHazeProvider { hazeLayers ->
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = Color.Transparent,
                bottomBar = {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = AppColors.screenBackground,
                        tonalElevation = 0.dp
                    ) {
                        Column {
                            LinearProgressIndicator(
                                progress = { (pagerState.currentPage + 1).toFloat() / totalSteps },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp),
                                color = AppColors.primary,
                                trackColor = AppColors.divider.copy(alpha = 0.1f)
                            )

                            Row(
                                modifier = Modifier
                                    .padding(AeonTheme.dimensions.spacingXl)
                                    .fillMaxWidth()
                                    .navigationBarsPadding(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                              ) {
                                // In Seed step, we don't use the standard NEXT button if they choose to seed
                                val isSeedStep = uiState.showDebugSeedStep && pagerState.currentPage == 0

                                if (!isSeedStep) {
                                    Button(
                                        onClick = {
                                            if (pagerState.currentPage < totalSteps - 1) {
                                                scope.launch {
                                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                                }
                                            } else {
                                                onEvent(OnboardingEvent.CompleteOnboarding)
                                            }
                                        },
                                        enabled = !uiState.isCompleting && !uiState.isSeeding,
                                        modifier = Modifier.height(56.dp),
                                        shape = RoundedCornerShape(AeonTheme.dimensions.buttonCornerRadius),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = AppColors.primary,
                                            contentColor = AppColors.onPrimary
                                        ),
                                        contentPadding = PaddingValues(horizontal = 24.dp)
                                    ) {
                                        if (uiState.isCompleting) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp),
                                                color = AppColors.onPrimary,
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Text(
                                                text = if (pagerState.currentPage < totalSteps - 1) "NEXT" else "GET STARTED",
                                                style = AppTextStyles.drawerItemLabel.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = 1.sp
                                                )
                                            )
                                            Spacer(Modifier.width(AeonTheme.dimensions.spacingSm))
                                            Icon(
                                                imageVector = if (pagerState.currentPage < totalSteps - 1) Icons.AutoMirrored.Rounded.ArrowForward else Icons.Rounded.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                } else {
                                    // On Step 0 (Seed Step), we show skip or seed
                                    Row(horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)) {
                                        Button(
                                            onClick = {
                                                scope.launch {
                                                    pagerState.animateScrollToPage(1)
                                                }
                                            },
                                            modifier = Modifier.height(56.dp),
                                            shape = RoundedCornerShape(AeonTheme.dimensions.buttonCornerRadius),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = AppColors.cardSurface,
                                                contentColor = AppColors.contentPrimary
                                            )
                                        ) {
                                            Text("SKIP SEEDING", style = AppTextStyles.listSectionHeader)
                                        }

                                        Button(
                                            onClick = { onEvent(OnboardingEvent.SeedDataRequested) },
                                            enabled = !uiState.isSeeding,
                                            modifier = Modifier.height(56.dp),
                                            shape = RoundedCornerShape(AeonTheme.dimensions.buttonCornerRadius),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = AppColors.primary,
                                                contentColor = AppColors.onPrimary
                                            )
                                        ) {
                                            if (uiState.isSeeding) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(24.dp),
                                                    color = AppColors.onPrimary,
                                                    strokeWidth = 2.dp
                                                )
                                            } else {
                                                Text("SEED DEBUG DATA", style = AppTextStyles.listSectionHeader)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AppColors.screenBackground)
                ) {
                    ContourBackground()

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxSize()
                            .hazeSource(hazeLayers.states[HazeLayer.Overlay])
                            .padding(paddingValues),
                        beyondViewportPageCount = 1,
                        userScrollEnabled = !uiState.isSeeding && !uiState.isCompleting
                    ) { page ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = AeonTheme.dimensions.spacingXl)
                        ) {
                            val actualPage = if (uiState.showDebugSeedStep) page - 1 else page

                            if (uiState.showDebugSeedStep && page == 0) {
                                SeedStep()
                            } else {
                                when (actualPage) {
                                    0 -> WelcomeStep()
                                    1 -> ProfileStep(
                                        userName = uiState.userName,
                                        onNameChanged = { onEvent(OnboardingEvent.UserNameChanged(it)) }
                                    )

                                    2 -> ProviderStep(
                                        selectedProviderId = uiState.selectedProviderId,
                                        availableProviders = uiState.availableProviders,
                                        onProviderSelected = { onEvent(OnboardingEvent.ProviderSelected(it)) }
                                    )

                                    3 -> CurrenciesStep(
                                        selectedCurrency = uiState.selectedCurrency,
                                        shortlistedCurrencies = uiState.shortlistedCurrencies,
                                        availableCurrencies = uiState.availableCurrencies,
                                        onCurrencySelected = { onEvent(OnboardingEvent.CurrencySelected(it)) },
                                        onShortlistChanged = {
                                            onEvent(
                                                OnboardingEvent.ShortlistedCurrenciesChanged(
                                                    it
                                                )
                                            )
                                        }
                                    )

                                    4 -> AppearanceStep(
                                        selectedTheme = uiState.selectedTheme,
                                        onThemeSelected = { onEvent(OnboardingEvent.ThemeSelected(it)) },
                                        selectedPrecision = uiState.decimalPrecision,
                                        onPrecisionSelected = { onEvent(OnboardingEvent.PrecisionSelected(it)) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            AeonTopAppBar(
                left = AeonTopBarSection(
                    content = {
                        AeonTopBarNavTitlePill(
                            title = "Step ${pagerState.currentPage + 1} of $totalSteps",
                            onBackClick = if (pagerState.currentPage > 0) {
                                {
                                    scope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                    }
                                }
                            } else null
                        )
                    }
                )
            )
        }
    }
}

@Composable
private fun StepHeader(icon: ImageVector, title: String, description: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)
    ) {
        Surface(
            modifier = Modifier.size(64.dp),
            shape = RoundedCornerShape(16.dp),
            color = AppColors.primary.copy(alpha = 0.1f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = AppColors.primary
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = title,
            style = AppTextStyles.heroAmount,
            color = AppColors.contentPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            style = AppTextStyles.listItemTitle,
            color = AppColors.contentSecondary.copy(alpha = 0.7f),
            lineHeight = 24.sp
        )
    }
}

@Composable
private fun SeedStep() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Surface(
            modifier = Modifier.size(120.dp),
            shape = CircleShape,
            color = AppColors.primary.copy(alpha = 0.1f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.BugReport,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = AppColors.primary
                )
            }
        }
        Spacer(modifier = Modifier.height(48.dp))
        Text(
            text = "Debug Mode Detected",
            style = AppTextStyles.heroAmountDisplay,
            color = AppColors.contentPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Would you like to seed the database with sample transactions, accounts, and budgets? This will skip the manual setup.",
            style = AppTextStyles.listItemTitle,
            color = AppColors.contentSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(scrollBottomPadding()))
    }
}

@Composable
private fun WelcomeStep() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Surface(
            modifier = Modifier.size(120.dp),
            shape = CircleShape,
            color = AppColors.primary.copy(alpha = 0.1f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.AccountBalanceWallet,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = AppColors.primary
                )
            }
        }
        Spacer(modifier = Modifier.height(48.dp))
        Text(
            text = "Welcome to OssWallet",
            style = AppTextStyles.heroAmountDisplay,
            color = AppColors.contentPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Your simple and secure personal finance manager. Let's get you set up in a few quick steps.",
            style = AppTextStyles.listItemTitle,
            color = AppColors.contentSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(scrollBottomPadding()))
    }
}

@Composable
private fun ProfileStep(
    userName: String,
    onNameChanged: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        StepHeader(
            icon = Icons.Rounded.Person,
            title = "Personalize",
            description = "How should we address you? This name will be used for your primary profile."
        )

        EntryEditorialInput(
            value = userName,
            onValueChange = onNameChanged,
            label = "YOUR NAME",
            icon = Icons.Rounded.Badge,
            placeholder = "Enter your name",
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(scrollBottomPadding()))
    }
}

@Composable
private fun ProviderStep(
    selectedProviderId: String,
    availableProviders: List<ExchangeRateProvider>,
    onProviderSelected: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            StepHeader(
                icon = Icons.Rounded.Public,
                title = "Rate Provider",
                description = "Select a source for your currency exchange rates. Each provider supports a different set of currencies."
            )

            availableProviders.forEach { provider ->
                val isSelected = provider.id == selectedProviderId
                Surface(
                    onClick = { onProviderSelected(provider.id) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) AppColors.primary.copy(alpha = 0.08f) else AppColors.cardSurface,
                    border = BorderStroke(
                        width = if (isSelected) 1.5.dp else 0.5.dp,
                        color = if (isSelected) AppColors.primary.copy(alpha = 0.5f) else AppColors.divider.copy(
                            alpha = 0.15f
                        )
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = CircleShape,
                            color = if (isSelected) AppColors.primary.copy(alpha = 0.15f) else AppColors.recessedSurface
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.Api,
                                    contentDescription = null,
                                    tint = if (isSelected) AppColors.primary else AppColors.contentSecondary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = provider.name,
                                style = AppTextStyles.cardSectionTitle,
                                color = if (isSelected) AppColors.primary else AppColors.contentPrimary
                            )
                            Text(
                                text = provider.attribution,
                                style = AppTextStyles.caption,
                                color = AppColors.contentSecondary.copy(alpha = 0.6f)
                            )
                        }
                        if (isSelected) {
                            Icon(
                                Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = AppColors.primary
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(scrollBottomPadding()))
        }
    }
}

@Composable
private fun CurrenciesStep(
    selectedCurrency: String,
    shortlistedCurrencies: Set<String>,
    availableCurrencies: List<CurrencyMetadata>,
    onCurrencySelected: (String) -> Unit,
    onShortlistChanged: (Set<String>) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredCurrencies = remember(searchQuery, availableCurrencies) {
        availableCurrencies.filter {
            it.code.contains(searchQuery, ignoreCase = true) ||
                    it.name.contains(searchQuery, ignoreCase = true)
        }
    }
    val filteredCodes = remember(filteredCurrencies) { filteredCurrencies.map { it.code }.toSet() }
    val haptic = LocalHapticFeedback.current

    Column(modifier = Modifier.fillMaxSize()) {
        StepHeader(
            icon = Icons.Rounded.Payments,
            title = "Currencies",
            description = "Set your base currency and select others you use regularly."
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
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
                        Icon(
                            Icons.Rounded.Clear,
                            contentDescription = "Clear",
                            modifier = Modifier.size(18.dp)
                        )
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onShortlistChanged(shortlistedCurrencies + filteredCodes)
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.cardSurface,
                    contentColor = AppColors.primary
                )
            ) {
                Icon(
                    Icons.Rounded.SelectAll,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("All", style = AppTextStyles.caption)
            }
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onShortlistChanged(shortlistedCurrencies - filteredCodes)
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.cardSurface,
                    contentColor = AppColors.contentSecondary
                )
            ) {
                Icon(
                    Icons.Rounded.Deselect,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("None", style = AppTextStyles.caption)
            }
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val toggled = shortlistedCurrencies.toMutableSet()
                    filteredCodes.forEach { code ->
                        if (code in toggled) toggled.remove(code) else toggled.add(code)
                    }
                    onShortlistChanged(toggled)
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.cardSurface,
                    contentColor = AppColors.contentSecondary
                )
            ) {
                Icon(Icons.Rounded.Flip, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Invert", style = AppTextStyles.caption)
            }
        }

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
                contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filteredCurrencies, key = { it.code }) { metadata ->
                    val isBase = metadata.code == selectedCurrency
                    val isShortlisted = metadata.code in shortlistedCurrencies

                    Surface(
                        onClick = {
                            if (isBase) {
                                // Already base, maybe show a hint?
                            } else {
                                onCurrencySelected(metadata.code)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isBase) AppColors.primary.copy(alpha = 0.08f) else AppColors.transparent,
                        modifier = Modifier.padding(horizontal = 8.dp)
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
                                color = if (isBase) AppColors.primary.copy(alpha = 0.2f) else AppColors.recessedSurface
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        getCurrencySymbol(metadata.code),
                                        style = AppTextStyles.cardSectionTitle,
                                        color = if (isBase) AppColors.primary else AppColors.contentSecondary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        metadata.name,
                                        style = AppTextStyles.listItemTitle.copy(fontWeight = if (isBase) FontWeight.Bold else FontWeight.SemiBold)
                                    )
                                    if (isBase) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            color = AppColors.primary.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                "BASE",
                                                modifier = Modifier.padding(
                                                    horizontal = 4.dp,
                                                    vertical = 2.dp
                                                ),
                                                style = AppTextStyles.caption.copy(
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = AppColors.primary
                                            )
                                        }
                                    }
                                }
                                Text(
                                    "${metadata.code} • ${metadata.country}",
                                    style = AppTextStyles.transactionSwipeAction,
                                    color = AppColors.contentSecondary.copy(alpha = 0.6f)
                                )
                            }
                            Checkbox(
                                checked = isShortlisted,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        onShortlistChanged(shortlistedCurrencies + metadata.code)
                                    } else if (!isBase) {
                                        onShortlistChanged(shortlistedCurrencies - metadata.code)
                                    }
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = AppColors.primary,
                                    uncheckedColor = AppColors.divider
                                )
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun AppearanceStep(
    selectedTheme: String,
    onThemeSelected: (String) -> Unit,
    selectedPrecision: Int,
    onPrecisionSelected: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        StepHeader(
            icon = Icons.Rounded.Palette,
            title = "Preferences",
            description = "Customize how OssWallet looks and handles your money."
        )

        Text("APP THEME", style = AppTextStyles.formSectionLabel, color = AppColors.primary)
        Spacer(modifier = Modifier.height(12.dp))

        val themes = listOf(
            Triple("System", "Matches device", Icons.Rounded.SettingsSuggest),
            Triple("Light", "Clean & bright", Icons.Rounded.LightMode),
            Triple("Dark", "Easy on eyes", Icons.Rounded.DarkMode),
            Triple("Amoled", "Pure black", Icons.Rounded.Contrast)
        )

        themes.chunked(2).forEach { chunk ->
            Row(
                modifier = Modifier.padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                chunk.forEach { (name, _, icon) ->
                    val isSelected = selectedTheme == name
                    Surface(
                        onClick = { onThemeSelected(name) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) AppColors.primary.copy(alpha = 0.08f) else AppColors.cardSurface,
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 0.5.dp,
                            color = if (isSelected) AppColors.primary.copy(alpha = 0.5f) else AppColors.divider.copy(
                                alpha = 0.15f
                            )
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                icon,
                                contentDescription = null,
                                tint = if (isSelected) AppColors.primary else AppColors.contentSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                name,
                                style = AppTextStyles.chipLabelBold,
                                color = if (isSelected) AppColors.primary else AppColors.contentPrimary
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text("DECIMAL PRECISION", style = AppTextStyles.formSectionLabel, color = AppColors.primary)
        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            listOf(2, 3).forEach { p ->
                val isSelected = selectedPrecision == p
                Surface(
                    onClick = { onPrecisionSelected(p) },
                    modifier = Modifier
                        .weight(1f)
                        .height(80.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) AppColors.primary.copy(alpha = 0.08f) else AppColors.cardSurface,
                    border = BorderStroke(
                        width = if (isSelected) 1.5.dp else 0.5.dp,
                        color = if (isSelected) AppColors.primary.copy(alpha = 0.5f) else AppColors.divider.copy(
                            alpha = 0.15f
                        )
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            p.toString(),
                            style = AppTextStyles.heroAmount,
                            color = if (isSelected) AppColors.primary else AppColors.contentPrimary
                        )
                        Text(
                            "PLACES",
                            style = AppTextStyles.caption,
                            color = if (isSelected) AppColors.primary.copy(alpha = 0.6f) else AppColors.contentSecondary.copy(
                                alpha = 0.6f
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = AppColors.recessedSurface.copy(alpha = 0.5f)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Rounded.Info,
                    contentDescription = null,
                    tint = AppColors.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Example: ${formatAuto(BigDecimal("1234.5678"), precision = selectedPrecision)}",
                    style = AppTextStyles.listItemTitle.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    ),
                    color = AppColors.contentPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(scrollBottomPadding()))
    }
}

@Preview(showBackground = true)
@Composable
fun WelcomeStepPreview() {
    OssWalletTheme {
        Box(modifier = Modifier.padding(AeonTheme.dimensions.spacingXl)) {
            WelcomeStep()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileStepPreview() {
    OssWalletTheme {
        Box(modifier = Modifier.padding(AeonTheme.dimensions.spacingXl)) {
            ProfileStep(userName = "John Doe", onNameChanged = {})
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProviderStepPreview() {
    val providers = listOf(
        object : ExchangeRateProvider {
            override val id = "exchangerate-api"
            override val name = "ExchangeRate-API"
            override val attribution = "Data from ExchangeRate-API"
            override suspend fun fetchSupportedCurrencies() = emptyList<CurrencyMetadata>()
            override suspend fun fetchRate(base: String, target: String): Result<Double> =
                Result.success(1.0)

            override suspend fun fetchAllRates(base: String): Result<Map<String, Double>> =
                Result.success(emptyMap())

            override suspend fun convertCurrencyAmount(
                amount: Double,
                from: String,
                to: String
            ): Result<Double> = Result.success(amount)
        }
    )
    OssWalletTheme {
        Box(modifier = Modifier.padding(AeonTheme.dimensions.spacingXl)) {
            ProviderStep(
                selectedProviderId = "exchangerate-api",
                availableProviders = providers,
                onProviderSelected = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CurrenciesStepPreview() {
    val currencies = listOf(
        CurrencyMetadata("USD", "United States Dollar", "United States"),
        CurrencyMetadata("EUR", "Euro", "European Union"),
        CurrencyMetadata("GBP", "British Pound", "United Kingdom"),
        CurrencyMetadata("JPY", "Japanese Yen", "Japan")
    )
    OssWalletTheme {
        Box(modifier = Modifier.padding(AeonTheme.dimensions.spacingXl)) {
            CurrenciesStep(
                selectedCurrency = "USD",
                shortlistedCurrencies = setOf("USD", "EUR"),
                availableCurrencies = currencies,
                onCurrencySelected = {},
                onShortlistChanged = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AppearanceStepPreview() {
    OssWalletTheme {
        Box(modifier = Modifier.padding(AeonTheme.dimensions.spacingXl)) {
            AppearanceStep(
                selectedTheme = "System",
                onThemeSelected = {},
                selectedPrecision = 2,
                onPrecisionSelected = {}
            )
        }
    }
}
