package com.osscabinet.ossWallet.ui.screens.transactions.entry

import androidx.lifecycle.SavedStateHandle
import com.osscabinet.ossWallet.core.util.MainDispatcherRule
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.AccountRef
import com.osscabinet.ossWallet.domain.model.AccountStructureType
import com.osscabinet.ossWallet.domain.model.EntryMode
import com.osscabinet.ossWallet.domain.model.Tag
import com.osscabinet.ossWallet.domain.model.isOutside
import com.osscabinet.ossWallet.domain.repository.ComputationRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.TagRepository
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot
import com.osscabinet.ossWallet.domain.service.management.TransactionManager
import com.osscabinet.ossWallet.domain.usecase.TransactionUseCases
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionEntryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private lateinit var transactionUseCases: TransactionUseCases
    private lateinit var transactionManager: TransactionManager
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var computationRepository: ComputationRepository
    private lateinit var tagRepository: TagRepository

    private lateinit var viewModel: TransactionEntryViewModel

    private val mockAccount = Account(
        id = "acc1",
        name = "Cash",
        currencyCode = "USD",
        structureType = AccountStructureType.CASH,
        colorHex = "#000000",
        icon = "ic"
    )

    private val mockToAccount = Account(
        id = "acc2",
        name = "Bank",
        currencyCode = "EUR",
        structureType = AccountStructureType.BANK,
        colorHex = "#FFFFFF",
        icon = "ic"
    )

    @Before
    fun setup() {
        CurrencyService.setFormatConfig(2, CurrencyService.FormattingSystem.WESTERN)

        transactionUseCases = mockk(relaxed = true)
        transactionManager = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)
        computationRepository = mockk(relaxed = true)
        tagRepository = mockk(relaxed = true)

        every { settingsRepository.currentProfileId } returns MutableStateFlow(1)
        every { settingsRepository.currency } returns flowOf("USD")
        every { settingsRepository.mapsAccessEnabled } returns flowOf(false)
        every { computationRepository.exchangeRatesSnapshot } returns MutableStateFlow(ExchangeRateSnapshot("USD", mapOf("EUR" to BigDecimal("0.85"))))
        every { transactionUseCases.observeEntryCoreData(any()) } returns flowOf(
            TransactionUseCases.EntryCoreData(
                accounts = listOf(mockAccount, mockToAccount),
                categories = emptyList(),
                tags = emptyList(),
                templates = emptyList(),
                payees = emptyList(),
                notes = emptyList()
            )
        )

        viewModel = TransactionEntryViewModel(
            savedStateHandle = SavedStateHandle(),
            transactionUseCases = transactionUseCases,
            transactionManager = transactionManager,
            settingsRepository = settingsRepository,
            computationRepository = computationRepository,
            tagRepository = tagRepository
        )
    }

    private suspend fun awaitState(predicate: (TransactionEntryState) -> Boolean): TransactionEntryState {
        return viewModel.state.filter(predicate).first()
    }

    private fun String.toBD() = toBigDecimalOrNull() ?: BigDecimal.ZERO

    @Test
    fun `initial state is correct`() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.initialize() }
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()
        
        val state = awaitState { it.accounts.isNotEmpty() }
        assertEquals(EntryMode.EXPENSE, state.entryMode)
        assertEquals("0", state.amount)
    }

    @Test
    fun `outside wallet inherits currency from other side and enforces sync`() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.initialize() }
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()

        viewModel.onEvent(TransactionEntryEvent.SetTransactionType(EntryMode.TRANSFER))
        advanceUntilIdle()
        
        // 1. Source (EUR) -> Outside (should be EUR)
        viewModel.onEvent(TransactionEntryEvent.SetAccount(mockToAccount)) // acc2 is EUR
        advanceUntilIdle()
        
        val state1 = awaitState { it.destination.account.isOutside && it.destination.currency == "EUR" }
        assertEquals("EUR", state1.destination.currency)
        
        // 2. Change Source to USD -> Target (Outside) should follow to USD
        viewModel.onEvent(TransactionEntryEvent.SetAccount(mockAccount)) // acc1 is USD
        advanceUntilIdle()
        
        val state2 = awaitState { state -> (state.source.account as? AccountRef.Wallet)?.id == "acc1" && state.source.currency == "USD" }
        assertEquals("USD", state2.destination.currency)

        // 3. Manually change Source currency to EUR -> Target (Outside) should follow
        viewModel.onEvent(TransactionEntryEvent.SetCurrency("EUR"))
        advanceUntilIdle()
        
        val state3 = awaitState { it.source.currency == "EUR" }
        assertEquals("EUR", state3.destination.currency)
    }

    @Test
    fun `same source and target currency results in 1 to 1 rate`() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.initialize() }
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()

        viewModel.onEvent(TransactionEntryEvent.SetTransactionType(EntryMode.TRANSFER))
        advanceUntilIdle()
        
        viewModel.onEvent(TransactionEntryEvent.SetAccount(mockAccount))
        advanceUntilIdle()
        
        viewModel.onEvent(TransactionEntryEvent.SetAmount("100"))
        advanceUntilIdle()
        
        val state = awaitState { 
            it.amount.toBD().compareTo(BigDecimal("100")) == 0 && 
            it.targetAmount.toBD().compareTo(BigDecimal("100")) == 0 && 
            it.targetCurrency == "USD" 
        }
        
        assertEquals("USD", state.selectedCurrency)
        assertEquals("USD", state.targetCurrency)
        assertEquals(0, BigDecimal("100").compareTo(state.targetAmount.toBD()))
    }

    @Test
    fun `swapping accounts in auto mode correctly swaps values and recalculates target`() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.initialize() }
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()

        viewModel.onEvent(TransactionEntryEvent.SetTransactionType(EntryMode.TRANSFER))
        advanceUntilIdle()
        
        viewModel.onEvent(TransactionEntryEvent.SetAccount(mockAccount)) // USD
        viewModel.onEvent(TransactionEntryEvent.SetTargetAccount(mockToAccount)) // EUR (0.85)
        advanceUntilIdle()
        
        viewModel.onEvent(TransactionEntryEvent.SetAmount("100"))
        advanceUntilIdle()
        
        viewModel.onEvent(TransactionEntryEvent.SwapAccounts)
        advanceUntilIdle()
        
        val state = awaitState { (it.source.account as? AccountRef.Wallet)?.id == "acc2" && it.amount.toBD().compareTo(BigDecimal("85")) == 0 }
        
        assertEquals("acc2", (state.source.account as? AccountRef.Wallet)?.id)
        assertEquals("EUR", state.selectedCurrency)
        assertEquals("acc1", (state.destination.account as? AccountRef.Wallet)?.id)
        assertEquals("USD", state.targetCurrency)
        
        val finalState = awaitState { it.targetAmount.toBD().compareTo(BigDecimal("100")) == 0 }
        assertEquals(0, BigDecimal("100").compareTo(finalState.targetAmount.toBD()))
        advanceUntilIdle()
    }

    @Test
    fun `SetAmount updates target and SetTargetAmount updates source`() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.initialize() }
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()

        viewModel.onEvent(TransactionEntryEvent.SetTransactionType(EntryMode.TRANSFER))
        advanceUntilIdle()
        
        viewModel.onEvent(TransactionEntryEvent.SetAccount(mockAccount)) // USD
        viewModel.onEvent(TransactionEntryEvent.SetTargetAccount(mockToAccount)) // EUR (0.85)
        advanceUntilIdle()
        
        viewModel.onEvent(TransactionEntryEvent.SetAmount("100"))
        advanceUntilIdle()
        
        val state1 = awaitState { it.targetAmount.toBD().compareTo(BigDecimal("85")) == 0 }
        assertEquals(0, BigDecimal("85").compareTo(state1.targetAmount.toBD()))
        
        viewModel.onEvent(TransactionEntryEvent.SetTargetAmount("170"))
        advanceUntilIdle()
        
        val state2 = awaitState { it.amount.toBD().compareTo(BigDecimal("200")) == 0 }
        assertEquals(0, BigDecimal("200").compareTo(state2.amount.toBD()))
        advanceUntilIdle()
    }

    @Test
    fun `ToggleRateMode correctly decouples amounts`() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.initialize() }
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()

        viewModel.onEvent(TransactionEntryEvent.SetTransactionType(EntryMode.TRANSFER))
        viewModel.onEvent(TransactionEntryEvent.SetAccount(mockAccount)) // USD
        viewModel.onEvent(TransactionEntryEvent.SetTargetAccount(mockToAccount)) // EUR (0.85)
        viewModel.onEvent(TransactionEntryEvent.SetAmount("100"))
        advanceUntilIdle()

        // Enable Fixed mode
        viewModel.onEvent(TransactionEntryEvent.ToggleRateMode)
        advanceUntilIdle()
        
        val state1 = awaitState { it.isRateFixed }
        assertTrue(state1.isRateFixed)

        // Change source - target should NOT change
        viewModel.onEvent(TransactionEntryEvent.SetAmount("200"))
        advanceUntilIdle()
        
        val state2 = awaitState { it.amount.toBD().compareTo(BigDecimal("200")) == 0 }
        assertEquals(0, BigDecimal("85").compareTo(state2.targetAmount.toBD()))
        
        // Implied rate should update (85 / 200 = 0.425)
        assertEquals(0, BigDecimal("0.425").compareTo(state2.exchangeRate))

        // Toggle back to Auto - target should re-align (200 * 0.85 = 170)
        viewModel.onEvent(TransactionEntryEvent.ToggleRateMode)
        advanceUntilIdle()

        val state3 = awaitState { !it.isRateFixed }
        assertFalse(state3.isRateFixed)
        val state4 = awaitState { it.targetAmount.toBD().compareTo(BigDecimal("170")) == 0 }
        assertEquals(0, BigDecimal("170").compareTo(state4.targetAmount.toBD()))
    }
}
