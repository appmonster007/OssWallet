package com.osscabinet.ossWallet.ui.screens.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
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
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.formatAmount
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.getCurrencyName
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.getCurrencySymbol
import com.osscabinet.ossWallet.domain.service.currency.provider.CurrencyMetadata
import com.osscabinet.ossWallet.domain.service.currency.provider.ExchangeRateProvider
import com.osscabinet.ossWallet.ui.components.entry.EntryEditorialInput
import com.osscabinet.ossWallet.ui.components.scrollBottomPadding
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import kotlinx.coroutines.launch
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onComplete: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState { 5 }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.onboardingCompleted.collect {
            onComplete()
        }
    }

    Scaffold(
        containerColor = AppColors.screenBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Step ${pagerState.currentPage + 1} of 5",
                        style = AppTextStyles.drawerItemLabel.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = AppColors.primary
                    )
                },
                navigationIcon = {
                    if (pagerState.currentPage > 0) {
                        IconButton(onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.transparent
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
                        progress = { (pagerState.currentPage + 1).toFloat() / 5 },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
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
                        Button(
                            onClick = {
                                if (pagerState.currentPage < 4) {
                                    scope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                    }
                                } else {
                                    viewModel.onEvent(OnboardingEvent.CompleteOnboarding)
                                }
                            },
                            enabled = !uiState.isCompleting,
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
                                    if (pagerState.currentPage < 4) "NEXT" else "GET STARTED",
                                    style = AppTextStyles.listSectionHeader
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    if (pagerState.currentPage < 4) Icons.AutoMirrored.Rounded.ArrowForward else Icons.Rounded.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
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
                .padding(paddingValues),
            userScrollEnabled = false
        ) { page ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = AeonTheme.dimensions.spacingXl)
            ) {
                when (page) {
                    0 -> WelcomeStep()
                    1 -> ProfileStep(
                        userName = uiState.userName,
                        onNameChanged = { viewModel.onEvent(OnboardingEvent.UserNameChanged(it)) }
                    )
                    2 -> ProviderStep(
                        selectedProviderId = uiState.selectedProviderId,
                        availableProviders = uiState.availableProviders,
                        onProviderSelected = { viewModel.onEvent(OnboardingEvent.ProviderSelected(it)) }
                    )
                    3 -> CurrencyStep(
                        selectedCurrency = uiState.selectedCurrency,
                        availableCurrencies = uiState.availableCurrencies,
                        onCurrencySelected = { viewModel.onEvent(OnboardingEvent.CurrencySelected(it)) }
                    )
                    4 -> AppearanceStep(
                        selectedTheme = uiState.selectedTheme,
                        onThemeSelected = { viewModel.onEvent(OnboardingEvent.ThemeSelected(it)) },
                        selectedPrecision = uiState.decimalPrecision,
                        onPrecisionSelected = { viewModel.onEvent(OnboardingEvent.PrecisionSelected(it)) }
                    )
                }
            }
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
private fun WelcomeStep() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
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
    }
}

@Composable
private fun ProfileStep(
    userName: String,
    onNameChanged: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
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
    }
}

@Composable
private fun ProviderStep(
    selectedProviderId: String,
    availableProviders: List<ExchangeRateProvider>,
    onProviderSelected: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        StepHeader(
            icon = Icons.Rounded.Public,
            title = "Rate Provider",
            description = "Select a source for your currency exchange rates. Each provider supports a different set of currencies."
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(availableProviders) { provider ->
                val isSelected = provider.id == selectedProviderId
                Surface(
                    onClick = { onProviderSelected(provider.id) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) AppColors.primary.copy(alpha = 0.08f) else AppColors.cardSurface,
                    border = BorderStroke(
                        width = if (isSelected) 1.5.dp else 0.5.dp,
                        color = if (isSelected) AppColors.primary.copy(alpha = 0.5f) else AppColors.divider.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.fillMaxWidth()
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
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = AppColors.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrencyStep(
    selectedCurrency: String,
    availableCurrencies: List<CurrencyMetadata>,
    onCurrencySelected: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredCurrencies = remember(searchQuery, availableCurrencies) {
        availableCurrencies.filter {
            it.code.contains(searchQuery, ignoreCase = true) ||
            it.name.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        StepHeader(
            icon = Icons.Rounded.Payments,
            title = "Base Currency",
            description = "Choose your primary currency for reporting. All balances will be converted to this currency."
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            placeholder = { Text("Search currencies...") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
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
            modifier = Modifier.weight(1f).fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = AppColors.cardSurface,
            border = BorderStroke(0.5.dp, AppColors.divider.copy(alpha = 0.15f))
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredCurrencies) { metadata ->
                    val isSelected = selectedCurrency == metadata.code
                    Surface(
                        onClick = { onCurrencySelected(metadata.code) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) AppColors.primary.copy(alpha = 0.1f) else AppColors.transparent,
                        border = if (isSelected) BorderStroke(0.5.dp, AppColors.primary.copy(alpha = 0.3f)) else null
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(40.dp),
                                shape = CircleShape,
                                color = if (isSelected) AppColors.primary.copy(alpha = 0.2f) else AppColors.recessedSurface
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        getCurrencySymbol(metadata.code),
                                        style = AppTextStyles.cardSectionTitle,
                                        color = if (isSelected) AppColors.primary else AppColors.contentSecondary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    metadata.name,
                                    style = AppTextStyles.listItemTitle.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold)
                                )
                                Text(
                                    "${metadata.code} • ${metadata.country}",
                                    style = AppTextStyles.transactionSwipeAction,
                                    color = AppColors.contentSecondary.copy(alpha = 0.6f)
                                )
                            }
                            if (isSelected) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = "Selected", tint = AppColors.primary, modifier = Modifier.size(20.dp))
                            }
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

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            themes.forEach { (name, desc, icon) ->
                val isSelected = selectedTheme == name
                Surface(
                    onClick = { onThemeSelected(name) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) AppColors.primary.copy(alpha = 0.08f) else AppColors.cardSurface,
                    border = BorderStroke(
                        width = if (isSelected) 1.5.dp else 0.5.dp,
                        color = if (isSelected) AppColors.primary.copy(alpha = 0.5f) else AppColors.divider.copy(alpha = 0.15f)
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
                        Text(name, style = AppTextStyles.chipLabelBold, color = if (isSelected) AppColors.primary else AppColors.contentPrimary)
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
                    modifier = Modifier.weight(1f).height(80.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) AppColors.primary.copy(alpha = 0.08f) else AppColors.cardSurface,
                    border = BorderStroke(
                        width = if (isSelected) 1.5.dp else 0.5.dp,
                        color = if (isSelected) AppColors.primary.copy(alpha = 0.5f) else AppColors.divider.copy(alpha = 0.15f)
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
                            color = if (isSelected) AppColors.primary.copy(alpha = 0.6f) else AppColors.contentSecondary.copy(alpha = 0.6f)
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
                Icon(Icons.Rounded.Info, contentDescription = null, tint = AppColors.primary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Example: ${formatAmount(BigDecimal("1234.5678"), selectedPrecision)}",
                    style = AppTextStyles.listItemTitle.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                    color = AppColors.contentPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(scrollBottomPadding()))
    }
}
