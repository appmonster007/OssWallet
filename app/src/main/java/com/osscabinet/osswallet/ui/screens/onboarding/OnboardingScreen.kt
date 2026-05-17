package com.osscabinet.ossWallet.ui.screens.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.service.currency.provider.CurrencyMetadata
import com.osscabinet.ossWallet.domain.service.currency.provider.ExchangeRateProvider
import com.osscabinet.ossWallet.ui.components.AeonHybridSelector
import com.osscabinet.ossWallet.ui.components.AeonSelectMode
import com.osscabinet.ossWallet.ui.components.entry.EntryEditorialInput
import com.osscabinet.ossWallet.ui.theme.AppColors
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onComplete: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { 5 })
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = AppColors.screenBackground,
        bottomBar = {
            OnboardingBottomBar(
                currentPage = pagerState.currentPage,
                onNext = {
                    if (pagerState.currentPage < 4) {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    } else {
                        viewModel.onEvent(OnboardingEvent.CompleteOnboarding)
                        onComplete()
                    }
                },
                onBack = {
                    scope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                    }
                }
            )
        }
    ) { paddingValues ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            userScrollEnabled = false
        ) { page ->
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
                    onThemeSelected = { viewModel.onEvent(OnboardingEvent.ThemeSelected(it)) }
                )
            }
        }
    }
}

@Composable
private fun WelcomeStep() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Welcome to OssWallet",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = AppColors.contentPrimary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Your simple and secure personal finance manager.",
            style = MaterialTheme.typography.bodyLarge,
            color = AppColors.contentSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
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
            .padding(32.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "What's your name?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = AppColors.contentPrimary
        )
        Spacer(modifier = Modifier.height(24.dp))
        EntryEditorialInput(
            value = userName,
            onValueChange = onNameChanged,
            label = "Name",
            icon = Icons.Rounded.Person,
            placeholder = "Enter your name",
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun CurrencyStep(
    selectedCurrency: String,
    availableCurrencies: List<CurrencyMetadata>,
    onCurrencySelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Choose your primary currency",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = AppColors.contentPrimary
        )
        Spacer(modifier = Modifier.height(24.dp))
        AeonHybridSelector(
            items = availableCurrencies,
            mode = AeonSelectMode.SINGLE,
            selectedIds = setOf(selectedCurrency),
            onSelectionChange = { ids -> ids.firstOrNull()?.let { onCurrencySelected(it) } },
            idMapper = { it.code },
            labelMapper = { it.name },
            detailMapper = { "${it.code} • ${it.country}" },
            iconMapper = { Icons.Rounded.Payments },
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Exchange Rate Provider",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = AppColors.contentPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Choose where to fetch currency data from.",
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.contentSecondary
        )
        Spacer(modifier = Modifier.height(24.dp))
        AeonHybridSelector(
            items = availableProviders,
            mode = AeonSelectMode.SINGLE,
            selectedIds = setOf(selectedProviderId),
            onSelectionChange = { ids -> ids.firstOrNull()?.let { onProviderSelected(it) } },
            idMapper = { it.id },
            labelMapper = { it.name },
            detailMapper = { it.attribution },
            iconMapper = { Icons.Rounded.Public },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun AppearanceStep(
    selectedTheme: String,
    onThemeSelected: (String) -> Unit
) {
    val themes = listOf("Light", "Dark", "System")
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Choose your appearance",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = AppColors.contentPrimary
        )
        Spacer(modifier = Modifier.height(24.dp))
        AeonHybridSelector(
            items = themes,
            mode = AeonSelectMode.SINGLE,
            selectedIds = setOf(selectedTheme),
            onSelectionChange = { ids -> ids.firstOrNull()?.let { onThemeSelected(it) } },
            idMapper = { it },
            labelMapper = { it },
            iconMapper = { Icons.Rounded.Palette },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun OnboardingBottomBar(
    currentPage: Int,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .navigationBarsPadding(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (currentPage > 0) {
            TextButton(onClick = onBack) {
                Text("Back", color = AppColors.contentSecondary)
            }
        } else {
            Spacer(modifier = Modifier.width(64.dp))
        }

        Button(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(
                containerColor = AppColors.accent,
                contentColor = AppColors.onAccent
            )
        ) {
            Text(if (currentPage == 4) "Get Started" else "Continue")
        }
    }
}
