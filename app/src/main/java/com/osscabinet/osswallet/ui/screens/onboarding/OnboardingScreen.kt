package com.osscabinet.ossWallet.ui.screens.onboarding

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.domain.service.currency.provider.CurrencyMetadata
import com.osscabinet.ossWallet.domain.service.currency.provider.ExchangeRateProvider
import com.osscabinet.ossWallet.ui.components.AeonHazeProvider
import com.osscabinet.ossWallet.ui.components.AeonTopAppBar
import com.osscabinet.ossWallet.ui.components.AeonTopBarNavTitlePill
import com.osscabinet.ossWallet.ui.components.AeonTopBarSection
import com.osscabinet.ossWallet.ui.components.aeonHazeSource
import com.osscabinet.ossWallet.ui.components.entry.EntryEditorialInput
import com.osscabinet.ossWallet.ui.components.scrollBottomPadding
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.HazeLayer
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.scaledDp
import com.osscabinet.ossWallet.ui.theme.scaledSp
import com.osscabinet.ossWallet.ui.util.CurrencyDisplaySpec
import com.osscabinet.ossWallet.ui.util.formatCurrency
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
    val configuration = LocalConfiguration.current
    val isSmallScreen = configuration.screenWidthDp < 360
    val uiScale = AeonTheme.uiScale

    AeonHazeProvider(backgroundColor = AppColors.screenBackground) { hazeLayers ->
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
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
                },
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
                                    .padding(
                                        horizontal = AeonTheme.dimensions.spacingXl,
                                        vertical = if (isSmallScreen) AeonTheme.dimensions.spacingLg else AeonTheme.dimensions.spacingXl
                                    )
                                    .fillMaxWidth()
                                    .navigationBarsPadding(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // In Seed step, we don't use the standard NEXT button if they choose to seed
                                val isSeedStep =
                                    uiState.showDebugSeedStep && pagerState.currentPage == 0

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
                                        modifier = Modifier.height(
                                            if (isSmallScreen) scaledDp(
                                                48f,
                                                uiScale
                                            ) else scaledDp(56f, uiScale)
                                        ),
                                        shape = RoundedCornerShape(AeonTheme.dimensions.buttonCornerRadius),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = AppColors.primary,
                                            contentColor = AppColors.onPrimary
                                        ),
                                        contentPadding = PaddingValues(
                                            horizontal = if (isSmallScreen) scaledDp(
                                                16f,
                                                uiScale
                                            ) else scaledDp(24f, uiScale)
                                        )
                                    ) {
                                        if (uiState.isCompleting) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                color = AppColors.onPrimary,
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Text(
                                                text = if (pagerState.currentPage < totalSteps - 1) "NEXT" else "GET STARTED",
                                                style = AppTextStyles.drawerItemLabel.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = 1.sp,
                                                    fontSize = if (isSmallScreen) scaledSp(
                                                        13f,
                                                        uiScale
                                                    ) else scaledSp(14f, uiScale)
                                                )
                                            )
                                            Spacer(Modifier.width(AeonTheme.dimensions.spacingSm))
                                            Icon(
                                                imageVector = if (pagerState.currentPage < totalSteps - 1) Icons.AutoMirrored.Rounded.ArrowForward else Icons.Rounded.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(if (isSmallScreen) 16.dp else 18.dp)
                                            )
                                        }
                                    }
                                } else {
                                    // On Step 0 (Seed Step), we show skip or seed
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
                                    ) {
                                        Button(
                                            onClick = {
                                                scope.launch {
                                                    pagerState.animateScrollToPage(1)
                                                }
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(
                                                    if (isSmallScreen) scaledDp(
                                                        48f,
                                                        uiScale
                                                    ) else scaledDp(56f, uiScale)
                                                ),
                                            shape = RoundedCornerShape(AeonTheme.dimensions.buttonCornerRadius),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = AppColors.cardSurface,
                                                contentColor = AppColors.contentPrimary
                                            ),
                                            contentPadding = PaddingValues(horizontal = 8.dp)
                                        ) {
                                            Text(
                                                "SKIP SEEDING",
                                                style = AppTextStyles.listSectionHeader.copy(
                                                    fontSize = if (isSmallScreen) 11.sp else 14.sp
                                                ),
                                                maxLines = 1,
                                                textAlign = TextAlign.Center
                                            )
                                        }

                                        Button(
                                            onClick = { onEvent(OnboardingEvent.SeedDataRequested) },
                                            enabled = !uiState.isSeeding,
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(
                                                    if (isSmallScreen) scaledDp(
                                                        48f,
                                                        uiScale
                                                    ) else scaledDp(56f, uiScale)
                                                ),
                                            shape = RoundedCornerShape(AeonTheme.dimensions.buttonCornerRadius),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = AppColors.primary,
                                                contentColor = AppColors.onPrimary
                                            ),
                                            contentPadding = PaddingValues(horizontal = 8.dp)
                                        ) {
                                            if (uiState.isSeeding) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(20.dp),
                                                    color = AppColors.onPrimary,
                                                    strokeWidth = 2.dp
                                                )
                                            } else {
                                                Text(
                                                    "SEED DEBUG DATA",
                                                    style = AppTextStyles.listSectionHeader.copy(
                                                        fontSize = if (isSmallScreen) 11.sp else 14.sp
                                                    ),
                                                    maxLines = 1,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            ) { paddingValues ->
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .aeonHazeSource(hazeLayers.states[HazeLayer.Overlay])
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
                                    onProviderSelected = {
                                        onEvent(
                                            OnboardingEvent.ProviderSelected(
                                                it
                                            )
                                        )
                                    }
                                )

                                3 -> CurrenciesStep(
                                    selectedCurrency = uiState.selectedCurrency,
                                    shortlistedCurrencies = uiState.shortlistedCurrencies,
                                    availableCurrencies = uiState.availableCurrencies,
                                    onCurrencySelected = {
                                        onEvent(
                                            OnboardingEvent.CurrencySelected(
                                                it
                                            )
                                        )
                                    },
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
                                    onPrecisionSelected = {
                                        onEvent(
                                            OnboardingEvent.PrecisionSelected(
                                                it
                                            )
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepHeader(icon: ImageVector, title: String, description: String) {
    val configuration = LocalConfiguration.current
    val isSmallScreen = configuration.screenWidthDp < 360
    val uiScale = AeonTheme.uiScale

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = if (isSmallScreen) scaledDp(16f, uiScale) else scaledDp(32f, uiScale))
    ) {
        Surface(
            modifier = Modifier.size(
                if (isSmallScreen) scaledDp(48f, uiScale) else scaledDp(
                    64f,
                    uiScale
                )
            ),
            shape = RoundedCornerShape(if (isSmallScreen) 12.dp else 16.dp),
            color = AppColors.primary.copy(alpha = 0.1f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(if (isSmallScreen) 24.dp else 32.dp),
                    tint = AppColors.primary
                )
            }
        }
        Spacer(
            modifier = Modifier.height(
                if (isSmallScreen) scaledDp(16f, uiScale) else scaledDp(
                    24f,
                    uiScale
                )
            )
        )
        Text(
            text = title,
            style = if (isSmallScreen) AppTextStyles.cardTitle else AppTextStyles.heroAmount,
            color = AppColors.contentPrimary
        )
        Spacer(
            modifier = Modifier.height(
                if (isSmallScreen) scaledDp(4f, uiScale) else scaledDp(
                    8f,
                    uiScale
                )
            )
        )
        Text(
            text = description,
            style = if (isSmallScreen) AppTextStyles.caption else AppTextStyles.listItemTitle,
            color = AppColors.contentSecondary.copy(alpha = 0.7f),
            lineHeight = if (isSmallScreen) scaledSp(20f, uiScale) else scaledSp(24f, uiScale)
        )
    }
}

@Composable
private fun SeedStep() {
    val configuration = LocalConfiguration.current
    val isSmallScreen = configuration.screenWidthDp < 360
    val uiScale = AeonTheme.uiScale

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(
            modifier = Modifier.height(
                if (isSmallScreen) scaledDp(16f, uiScale) else scaledDp(
                    32f,
                    uiScale
                )
            )
        )
        Surface(
            modifier = Modifier.size(
                if (isSmallScreen) scaledDp(80f, uiScale) else scaledDp(
                    120f,
                    uiScale
                )
            ),
            shape = CircleShape,
            color = AppColors.primary.copy(alpha = 0.1f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.BugReport,
                    contentDescription = null,
                    modifier = Modifier.size(if (isSmallScreen) 40.dp else 64.dp),
                    tint = AppColors.primary
                )
            }
        }
        Spacer(
            modifier = Modifier.height(
                if (isSmallScreen) scaledDp(32f, uiScale) else scaledDp(
                    48f,
                    uiScale
                )
            )
        )
        Text(
            text = "Debug Mode Detected",
            style = if (isSmallScreen) AppTextStyles.heroAmount else AppTextStyles.heroAmountDisplay,
            color = AppColors.contentPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(
            modifier = Modifier.height(
                if (isSmallScreen) scaledDp(
                    8f,
                    uiScale
                ) else scaledDp(16f, uiScale)
            )
        )
        Text(
            text = "Would you like to seed the database with sample transactions, accounts, and budgets? This will skip the manual setup.",
            style = if (isSmallScreen) AppTextStyles.caption else AppTextStyles.listItemTitle,
            color = AppColors.contentSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = scaledDp(16f, uiScale))
        )
        Spacer(modifier = Modifier.height(scrollBottomPadding()))
    }
}

@Composable
private fun WelcomeStep() {
    val configuration = LocalConfiguration.current
    val isSmallScreen = configuration.screenWidthDp < 360
    val uiScale = AeonTheme.uiScale

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(
            modifier = Modifier.height(
                if (isSmallScreen) scaledDp(16f, uiScale) else scaledDp(
                    32f,
                    uiScale
                )
            )
        )
        Surface(
            modifier = Modifier.size(
                if (isSmallScreen) scaledDp(80f, uiScale) else scaledDp(
                    120f,
                    uiScale
                )
            ),
            shape = CircleShape,
            color = AppColors.primary.copy(alpha = 0.1f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.AccountBalanceWallet,
                    contentDescription = null,
                    modifier = Modifier.size(if (isSmallScreen) 40.dp else 64.dp),
                    tint = AppColors.primary
                )
            }
        }
        Spacer(
            modifier = Modifier.height(
                if (isSmallScreen) scaledDp(32f, uiScale) else scaledDp(
                    48f,
                    uiScale
                )
            )
        )
        Text(
            text = "Welcome to OssWallet",
            style = if (isSmallScreen) AppTextStyles.heroAmount else AppTextStyles.heroAmountDisplay,
            color = AppColors.contentPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(
            modifier = Modifier.height(
                if (isSmallScreen) scaledDp(
                    8f,
                    uiScale
                ) else scaledDp(16f, uiScale)
            )
        )
        Text(
            text = "Your simple and secure personal finance manager. Let's get you set up in a few quick steps.",
            style = if (isSmallScreen) AppTextStyles.caption else AppTextStyles.listItemTitle,
            color = AppColors.contentSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = scaledDp(16f, uiScale))
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
    val configuration = LocalConfiguration.current
    val isSmallScreen = configuration.screenWidthDp < 360
    val uiScale = AeonTheme.uiScale

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
                description = "Select a source for your currency exchange rates."
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
                        .padding(
                            bottom = if (isSmallScreen) scaledDp(
                                8f,
                                uiScale
                            ) else scaledDp(12f, uiScale)
                        )
                ) {
                    Row(
                        modifier = Modifier.padding(
                            if (isSmallScreen) scaledDp(
                                12f,
                                uiScale
                            ) else scaledDp(20f, uiScale)
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(
                                if (isSmallScreen) scaledDp(
                                    40f,
                                    uiScale
                                ) else scaledDp(48f, uiScale)
                            ),
                            shape = CircleShape,
                            color = if (isSelected) AppColors.primary.copy(alpha = 0.15f) else AppColors.recessedSurface
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.Api,
                                    contentDescription = null,
                                    tint = if (isSelected) AppColors.primary else AppColors.contentSecondary,
                                    modifier = Modifier.size(if (isSmallScreen) 20.dp else 24.dp)
                                )
                            }
                        }
                        Spacer(
                            modifier = Modifier.width(
                                if (isSmallScreen) scaledDp(
                                    12f,
                                    uiScale
                                ) else scaledDp(16f, uiScale)
                            )
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = provider.name,
                                style = if (isSmallScreen) AppTextStyles.managementItemTitle else AppTextStyles.cardSectionTitle,
                                color = if (isSelected) AppColors.primary else AppColors.contentPrimary
                            )
                            Text(
                                text = provider.attribution,
                                style = AppTextStyles.caption,
                                color = AppColors.contentSecondary.copy(alpha = 0.6f),
                                fontSize = if (isSmallScreen) scaledSp(10f, uiScale) else scaledSp(
                                    11f,
                                    uiScale
                                )
                            )
                        }
                        if (isSelected) {
                            Icon(
                                Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = AppColors.primary,
                                modifier = Modifier.size(if (isSmallScreen) 20.dp else 24.dp)
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
    val configuration = LocalConfiguration.current
    val isSmallScreen = configuration.screenWidthDp < 360
    val uiScale = AeonTheme.uiScale

    // Suggested currencies for quick access
    val suggestedCodes = listOf("USD", "EUR", "GBP", "JPY", "CNY", "INR", "CAD", "AUD")
    val suggestedCurrencies = remember(availableCurrencies) {
        availableCurrencies.filter { it.code in suggestedCodes }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(
            if (isSmallScreen) scaledDp(
                4f,
                uiScale
            ) else scaledDp(8f, uiScale)
        )
    ) {
        if (!isSmallScreen || searchQuery.isEmpty()) {
            item {
                StepHeader(
                    icon = Icons.Rounded.Payments,
                    title = "Currencies",
                    description = if (isSmallScreen) "Pick your base and regular currencies." else "Set your base currency and select others you use regularly."
                )
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        bottom = if (isSmallScreen) scaledDp(8f, uiScale) else scaledDp(
                            16f,
                            uiScale
                        )
                    ),
                placeholder = {
                    Text(
                        "Search 150+ currencies...",
                        style = AppTextStyles.caption,
                        fontSize = if (isSmallScreen) scaledSp(12f, uiScale) else scaledSp(
                            14f,
                            uiScale
                        )
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Rounded.Search,
                        contentDescription = null,
                        modifier = Modifier.size(if (isSmallScreen) 18.dp else 20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                Icons.Rounded.Clear,
                                contentDescription = "Clear",
                                modifier = Modifier.size(if (isSmallScreen) 16.dp else 18.dp)
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = if (isSmallScreen) scaledSp(14f, uiScale) else scaledSp(16f, uiScale)
                ),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = AppColors.cardSurface,
                    focusedContainerColor = AppColors.cardSurface,
                    unfocusedIndicatorColor = AppColors.divider.copy(alpha = 0.1f),
                    focusedIndicatorColor = AppColors.primary.copy(alpha = 0.5f)
                )
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        bottom = if (isSmallScreen) scaledDp(4f, uiScale) else scaledDp(
                            8f,
                            uiScale
                        )
                    ),
                horizontalArrangement = Arrangement.spacedBy(scaledDp(8f, uiScale)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    Icons.Rounded.SelectAll to "All",
                    Icons.Rounded.Deselect to "None",
                    Icons.Rounded.Flip to "Invert"
                ).forEach { (icon, label) ->
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            when (label) {
                                "All" -> onShortlistChanged(shortlistedCurrencies + filteredCodes)
                                "None" -> onShortlistChanged(shortlistedCurrencies - filteredCodes)
                                "Invert" -> {
                                    val toggled = shortlistedCurrencies.toMutableSet()
                                    filteredCodes.forEach { code ->
                                        if (code in toggled) toggled.remove(code) else toggled.add(
                                            code
                                        )
                                    }
                                    onShortlistChanged(toggled)
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.cardSurface,
                            contentColor = if (label == "All") AppColors.primary else AppColors.contentSecondary
                        )
                    ) {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                        if (!isSmallScreen) {
                            Spacer(Modifier.width(4.dp))
                            Text(label, style = AppTextStyles.caption)
                        }
                    }
                }
            }
        }

        if (searchQuery.isEmpty() && suggestedCurrencies.isNotEmpty()) {
            item {
                Text(
                    "SUGGESTED",
                    style = AppTextStyles.formSectionLabel,
                    color = AppColors.primary,
                    modifier = Modifier.padding(vertical = scaledDp(8f, uiScale))
                )
            }
            items(suggestedCurrencies.chunked(if (isSmallScreen) 3 else 4)) { rowItems ->
                Row(horizontalArrangement = Arrangement.spacedBy(scaledDp(8f, uiScale))) {
                    rowItems.forEach { currency ->
                        CurrencyTile(
                            modifier = Modifier.weight(1f),
                            metadata = currency,
                            isBase = currency.code == selectedCurrency,
                            isShortlisted = currency.code in shortlistedCurrencies,
                            isSmallScreen = isSmallScreen,
                            onSelectBase = { onCurrencySelected(it) },
                            onToggleShortlist = { code, checked ->
                                if (checked) onShortlistChanged(shortlistedCurrencies + code)
                                else if (code != selectedCurrency) onShortlistChanged(
                                    shortlistedCurrencies - code
                                )
                            }
                        )
                    }
                    // Fill empty slots in the row
                    repeat(if (isSmallScreen) 3 - rowItems.size else 4 - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
            item {
                Text(
                    "ALL CURRENCIES",
                    style = AppTextStyles.formSectionLabel,
                    color = AppColors.contentSecondary.copy(alpha = 0.6f),
                    modifier = Modifier.padding(
                        top = scaledDp(16f, uiScale),
                        bottom = scaledDp(8f, uiScale)
                    )
                )
            }
        }

        items(filteredCurrencies.chunked(if (isSmallScreen) 3 else 4)) { rowItems ->
            Row(
                modifier = Modifier.padding(vertical = scaledDp(4f, uiScale)),
                horizontalArrangement = Arrangement.spacedBy(scaledDp(8f, uiScale))
            ) {
                rowItems.forEach { currency ->
                    CurrencyTile(
                        modifier = Modifier.weight(1f),
                        metadata = currency,
                        isBase = currency.code == selectedCurrency,
                        isShortlisted = currency.code in shortlistedCurrencies,
                        isSmallScreen = isSmallScreen,
                        onSelectBase = { onCurrencySelected(it) },
                        onToggleShortlist = { code, checked ->
                            if (checked) onShortlistChanged(shortlistedCurrencies + code)
                            else if (code != selectedCurrency) onShortlistChanged(
                                shortlistedCurrencies - code
                            )
                        }
                    )
                }
                repeat(if (isSmallScreen) 3 - rowItems.size else 4 - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(scrollBottomPadding()))
        }
    }
}

@Composable
private fun CurrencyTile(
    metadata: CurrencyMetadata,
    isBase: Boolean,
    isShortlisted: Boolean,
    isSmallScreen: Boolean,
    onSelectBase: (String) -> Unit,
    onToggleShortlist: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = { onSelectBase(metadata.code) },
        shape = RoundedCornerShape(16.dp),
        color = when {
            isBase -> AppColors.primary.copy(alpha = 0.08f)
            isShortlisted -> AppColors.cardSurface
            else -> AppColors.cardSurface.copy(alpha = 0.4f)
        },
        border = BorderStroke(
            width = if (isBase) 2.dp else 0.5.dp,
            color = if (isBase) AppColors.primary else AppColors.divider.copy(alpha = 0.1f)
        ),
        modifier = modifier.height(
            if (isSmallScreen) scaledDp(
                88f,
                AeonTheme.uiScale
            ) else scaledDp(100f, AeonTheme.uiScale)
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    CurrencyService.getCurrencySymbol(metadata.code),
                    style = if (isSmallScreen) AppTextStyles.cardTitle else AppTextStyles.heroAmount,
                    color = if (isBase) AppColors.primary else AppColors.contentPrimary
                )
                Text(
                    metadata.code,
                    style = AppTextStyles.caption.copy(fontWeight = FontWeight.Bold),
                    fontSize = if (isSmallScreen) 10.sp else 11.sp,
                    color = if (isBase) AppColors.primary else AppColors.contentSecondary
                )
            }

            Checkbox(
                checked = isShortlisted,
                onCheckedChange = { onToggleShortlist(metadata.code, it) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(if (isSmallScreen) 32.dp else 40.dp),
                colors = CheckboxDefaults.colors(
                    checkedColor = AppColors.primary,
                    uncheckedColor = AppColors.divider.copy(alpha = 0.5f)
                )
            )

            if (isBase) {
                Surface(
                    color = AppColors.primary,
                    shape = RoundedCornerShape(topStart = 4.dp, bottomEnd = 0.dp),
                    modifier = Modifier.align(Alignment.BottomEnd)
                ) {
                    Text(
                        "BASE",
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        style = AppTextStyles.caption.copy(
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black
                        ),
                        color = AppColors.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun AppearanceStep(
    selectedTheme: String,
    onThemeSelected: (String) -> Unit,
    selectedPrecision: Int,
    onPrecisionSelected: (Int) -> Unit
) {
    val configuration = LocalConfiguration.current
    val isSmallScreen = configuration.screenWidthDp < 360
    val uiScale = AeonTheme.uiScale

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

        Text(
            "APP THEME",
            style = AppTextStyles.formSectionLabel,
            color = AppColors.primary,
            fontSize = if (isSmallScreen) scaledSp(10f, uiScale) else scaledSp(11f, uiScale)
        )
        Spacer(
            modifier = Modifier.height(
                if (isSmallScreen) scaledDp(
                    8f,
                    uiScale
                ) else scaledDp(12f, uiScale)
            )
        )

        val themes = listOf(
            Triple("System", "Matches device", Icons.Rounded.SettingsSuggest),
            Triple("Light", "Clean & bright", Icons.Rounded.LightMode),
            Triple("Dark", "Easy on eyes", Icons.Rounded.DarkMode),
            Triple("Amoled", "Pure black", Icons.Rounded.Contrast)
        )

        themes.chunked(2).forEach { chunk ->
            Row(
                modifier = Modifier.padding(bottom = scaledDp(8f, uiScale)),
                horizontalArrangement = Arrangement.spacedBy(scaledDp(8f, uiScale))
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
                            modifier = Modifier.padding(
                                if (isSmallScreen) scaledDp(
                                    8f,
                                    uiScale
                                ) else scaledDp(12f, uiScale)
                            ),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                icon,
                                contentDescription = null,
                                tint = if (isSelected) AppColors.primary else AppColors.contentSecondary,
                                modifier = Modifier.size(if (isSmallScreen) 20.dp else 24.dp)
                            )
                            Spacer(
                                modifier = Modifier.height(
                                    if (isSmallScreen) scaledDp(
                                        4f,
                                        uiScale
                                    ) else scaledDp(8f, uiScale)
                                )
                            )
                            Text(
                                name,
                                style = if (isSmallScreen) AppTextStyles.caption.copy(fontWeight = FontWeight.Bold) else AppTextStyles.chipLabelBold,
                                color = if (isSelected) AppColors.primary else AppColors.contentPrimary
                            )
                        }
                    }
                }
            }
        }

        Spacer(
            modifier = Modifier.height(
                if (isSmallScreen) scaledDp(20f, uiScale) else scaledDp(
                    32f,
                    uiScale
                )
            )
        )

        Text(
            "DECIMAL PRECISION",
            style = AppTextStyles.formSectionLabel,
            color = AppColors.primary,
            fontSize = if (isSmallScreen) scaledSp(10f, uiScale) else scaledSp(11f, uiScale)
        )
        Spacer(
            modifier = Modifier.height(
                if (isSmallScreen) scaledDp(
                    8f,
                    uiScale
                ) else scaledDp(12f, uiScale)
            )
        )

        Row(horizontalArrangement = Arrangement.spacedBy(scaledDp(16f, uiScale))) {
            listOf(2, 3).forEach { p ->
                val isSelected = selectedPrecision == p
                Surface(
                    onClick = { onPrecisionSelected(p) },
                    modifier = Modifier
                        .weight(1f)
                        .height(
                            if (isSmallScreen) scaledDp(64f, uiScale) else scaledDp(
                                80f,
                                uiScale
                            )
                        ),
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
                            style = if (isSmallScreen) AppTextStyles.cardTitle else AppTextStyles.heroAmount,
                            color = if (isSelected) AppColors.primary else AppColors.contentPrimary
                        )
                        Text(
                            "PLACES",
                            style = AppTextStyles.caption,
                            fontSize = 9.sp,
                            color = if (isSelected) AppColors.primary.copy(alpha = 0.6f) else AppColors.contentSecondary.copy(
                                alpha = 0.6f
                            )
                        )
                    }
                }
            }
        }

        Spacer(
            modifier = Modifier.height(
                if (isSmallScreen) scaledDp(16f, uiScale) else scaledDp(
                    24f,
                    uiScale
                )
            )
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = AppColors.recessedSurface.copy(alpha = 0.5f)
        ) {
            Row(
                modifier = Modifier.padding(
                    if (isSmallScreen) scaledDp(12f, uiScale) else scaledDp(
                        16f,
                        uiScale
                    )
                ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Rounded.Info,
                    contentDescription = null,
                    tint = AppColors.primary,
                    modifier = Modifier.size(if (isSmallScreen) 14.dp else 16.dp)
                )
                Spacer(modifier = Modifier.width(scaledDp(8f, uiScale)))
                Text(
                    text = "Example: ${
                        formatCurrency(
                            BigDecimal("1234.5678"),
                            spec = CurrencyDisplaySpec(precision = selectedPrecision)
                        )
                    }",
                    style = AppTextStyles.listItemTitle.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = if (isSmallScreen) scaledSp(12f, uiScale) else scaledSp(
                            14f,
                            uiScale
                        )
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

@Preview(
    name = "Small Screen Onboarding - Welcome",
    device = "spec:width=320dp,height=533dp",
    showBackground = true
)
@Composable
fun OnboardingSmallScreenWelcomePreview() {
    OssWalletTheme {
        Box(modifier = Modifier.padding(AeonTheme.dimensions.spacingXl)) {
            WelcomeStep()
        }
    }
}

@Preview(
    name = "Small Screen Onboarding - Seed",
    device = "spec:width=320dp,height=533dp",
    showBackground = true
)
@Composable
fun OnboardingSmallScreenSeedPreview() {
    OssWalletTheme {
        Box(modifier = Modifier.padding(AeonTheme.dimensions.spacingXl)) {
            SeedStep()
        }
    }
}

@Preview(
    name = "Small Screen Onboarding - Profile",
    device = "spec:width=320dp,height=533dp",
    showBackground = true
)
@Composable
fun OnboardingSmallScreenProfilePreview() {
    OssWalletTheme {
        Box(modifier = Modifier.padding(AeonTheme.dimensions.spacingXl)) {
            ProfileStep(userName = "John", onNameChanged = {})
        }
    }
}

@Preview(
    name = "Small Screen Onboarding - Currencies",
    device = "spec:width=320dp,height=533dp",
    showBackground = true
)
@Composable
fun OnboardingSmallScreenCurrenciesPreview() {
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

@Preview(
    name = "Small Screen Onboarding - Provider",
    device = "spec:width=320dp,height=533dp",
    showBackground = true
)
@Composable
fun OnboardingSmallScreenProviderPreview() {
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

@Preview(
    name = "Small Screen Onboarding - Appearance",
    device = "spec:width=320dp,height=533dp",
    showBackground = true
)
@Composable
fun OnboardingSmallScreenAppearancePreview() {
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

@Preview(
    name = "Small Screen Onboarding Full",
    device = "spec:width=320dp,height=533dp",
    showBackground = true
)
@Composable
fun OnboardingSmallScreenFullPreview() {
    OssWalletTheme {
        OnboardingContent(
            uiState = OnboardingState(
                userName = "John Doe",
                availableProviders = listOf(
                    object : ExchangeRateProvider {
                        override val id = "exchangerate-api"
                        override val name = "ExchangeRate-API"
                        override val attribution = "Data from ExchangeRate-API"
                        override suspend fun fetchSupportedCurrencies() =
                            emptyList<CurrencyMetadata>()

                        override suspend fun fetchRate(
                            base: String,
                            target: String
                        ): Result<Double> =
                            Result.success(1.0)

                        override suspend fun fetchAllRates(base: String): Result<Map<String, Double>> =
                            Result.success(emptyMap())

                        override suspend fun convertCurrencyAmount(
                            amount: Double,
                            from: String,
                            to: String
                        ): Result<Double> = Result.success(amount)
                    }
                ),
                availableCurrencies = listOf(
                    CurrencyMetadata("USD", "United States Dollar", "United States"),
                    CurrencyMetadata("EUR", "Euro", "European Union")
                ),
                showDebugSeedStep = true
            ),
            onEvent = {}
        )
    }
}
