package com.osscabinet.ossWallet.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.Profile
import com.osscabinet.ossWallet.domain.repository.ProfileRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.service.currency.provider.CurrencyMetadata
import com.osscabinet.ossWallet.domain.service.currency.provider.ExchangeRateProvider
import com.osscabinet.ossWallet.domain.service.currency.provider.ExchangeRateProviderRegistry
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class OnboardingState(
    val userName: String = "",
    val selectedCurrency: String = "USD",
    val selectedProviderId: String = "exchangerate-api",
    val selectedTheme: String = "System",
    val decimalPrecision: Int = 2,
    val availableProviders: List<ExchangeRateProvider> = emptyList(),
    val availableCurrencies: List<CurrencyMetadata> = emptyList(),
    val isCompleting: Boolean = false
)

sealed class OnboardingEvent {
    data class UserNameChanged(val name: String) : OnboardingEvent()
    data class CurrencySelected(val currency: String) : OnboardingEvent()
    data class ProviderSelected(val providerId: String) : OnboardingEvent()
    data class ThemeSelected(val theme: String) : OnboardingEvent()
    data class PrecisionSelected(val precision: Int) : OnboardingEvent()
    object CompleteOnboarding : OnboardingEvent()
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val profileRepository: ProfileRepository,
    private val providerRegistry: ExchangeRateProviderRegistry
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingState(
        availableProviders = providerRegistry.getAllProviders()
    ))
    val uiState: StateFlow<OnboardingState> = _uiState.asStateFlow()

    private val _onboardingCompleted = MutableSharedFlow<Unit>()
    val onboardingCompleted: SharedFlow<Unit> = _onboardingCompleted.asSharedFlow()

    init {
        // Load initial currencies for the default provider
        viewModelScope.launch {
            val currencies = providerRegistry.getProvider(_uiState.value.selectedProviderId).fetchSupportedCurrencies()
            _uiState.update { it.copy(availableCurrencies = currencies) }
        }
    }

    fun onEvent(event: OnboardingEvent) {
        when (event) {
            is OnboardingEvent.UserNameChanged -> {
                _uiState.update { it.copy(userName = event.name) }
            }
            is OnboardingEvent.CurrencySelected -> {
                _uiState.update { it.copy(selectedCurrency = event.currency) }
            }
            is OnboardingEvent.ProviderSelected -> {
                viewModelScope.launch {
                    val currencies = providerRegistry.getProvider(event.providerId).fetchSupportedCurrencies()
                    _uiState.update { 
                        it.copy(
                            selectedProviderId = event.providerId,
                            availableCurrencies = currencies,
                            // Reset selected currency if not supported by new provider
                            selectedCurrency = if (currencies.any { c -> c.code == it.selectedCurrency }) it.selectedCurrency else (currencies.firstOrNull()?.code ?: "USD")
                        )
                    }
                }
            }
            is OnboardingEvent.ThemeSelected -> {
                _uiState.update { it.copy(selectedTheme = event.theme) }
                viewModelScope.launch {
                    settingsRepository.saveTheme(event.theme)
                }
            }
            is OnboardingEvent.PrecisionSelected -> {
                _uiState.update { it.copy(decimalPrecision = event.precision) }
            }
            OnboardingEvent.CompleteOnboarding -> {
                completeOnboarding()
            }
        }
    }

    private fun completeOnboarding() {
        if (_uiState.value.isCompleting) return
        
        _uiState.update { it.copy(isCompleting = true) }
        viewModelScope.launch {
            try {
                val state = _uiState.value
                settingsRepository.saveUserName(state.userName)
                settingsRepository.saveCurrency(state.selectedCurrency)
                settingsRepository.saveCurrencyProvider(state.selectedProviderId)
                settingsRepository.saveDecimalPrecision(state.decimalPrecision)
                
                // Create initial profile
                profileRepository.insertProfile(
                    Profile(
                        id = 1,
                        name = if (state.userName.isBlank()) "Default Profile" else "${state.userName}'s Profile",
                        baseCurrency = state.selectedCurrency
                    )
                )
                settingsRepository.saveCurrentProfileId(1)
                settingsRepository.saveOnboardingCompleted(true)
                
                _onboardingCompleted.emit(Unit)
            } catch (e: Exception) {
                _uiState.update { it.copy(isCompleting = false) }
            }
        }
    }
}
