package com.osscabinet.ossWallet.ui.screens.transactions.entry

import androidx.lifecycle.SavedStateHandle
import com.osscabinet.ossWallet.core.util.MainDispatcherRule
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.AccountRef
import com.osscabinet.ossWallet.domain.model.AccountStructureType
import com.osscabinet.ossWallet.domain.model.EntryMode
import com.osscabinet.ossWallet.domain.model.Tag
import com.osscabinet.ossWallet.domain.model.isOutside
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.repository.CategoryRepository
import com.osscabinet.ossWallet.domain.repository.ComputationRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.TagRepository
import com.osscabinet.ossWallet.domain.repository.TemplateRepository
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
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionEntryViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private lateinit var transactionUseCases: TransactionUseCases
    private lateinit var transactionManager: TransactionManager
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var computationRepository: ComputationRepository
    private lateinit var accountRepository: AccountRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var tagRepository: TagRepository
    private lateinit var templateRepository: TemplateRepository

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
        accountRepository = mockk(relaxed = true)
        categoryRepository = mockk(relaxed = true)
        tagRepository = mockk(relaxed = true)
        templateRepository = mockk(relaxed = true)

        every { settingsRepository.currentProfileId } returns MutableStateFlow(1)
        every { settingsRepository.currency } returns flowOf("USD")
        every { settingsRepository.mapsAccessEnabled } returns flowOf(false)
        
        every { accountRepository.getAllAccounts(any()) } returns MutableStateFlow(listOf(mockAccount, mockToAccount))
        every { categoryRepository.categories } returns MutableStateFlow(emptyList())
        every { tagRepository.getAllTags() } returns MutableStateFlow(emptyList<Tag>())
        every { computationRepository.exchangeRatesSnapshot } returns MutableStateFlow(ExchangeRateSnapshot("USD", mapOf("EUR" to BigDecimal("0.85"))))

        viewModel = TransactionEntryViewModel(
            savedStateHandle = SavedStateHandle(),
            transactionUseCases = transactionUseCases,
            transactionManager = transactionManager,
            settingsRepository = settingsRepository,
            computationRepository = computationRepository,
            accountRepository = accountRepository,
            categoryRepository = categoryRepository,
            tagRepository = tagRepository,
            templateRepository = templateRepository
        )
        
        viewModel.initialize()
    }

    private suspend fun awaitState(predicate: (TransactionEntryState) -> Boolean): TransactionEntryState {
        return viewModel.state.filter(predicate).first()
    }

    private fun String.toBD() = toBigDecimalOrNull() ?: BigDecimal.ZERO

    @Test
    fun `initial state is correct`() = runTest {
        backgroundScope.launch { viewModel.state.collect {} }
        val state = awaitState { it.accounts.isNotEmpty() }
        assertEquals(EntryMode.EXPENSE, state.entryMode)
        assertEquals("0", state.amount)
    }

    @Test
    fun `outside wallet inherits currency from other side and enforces sync`() = runTest {
        backgroundScope.launch { viewModel.state.collect {} }
        awaitState { it.accounts.isNotEmpty() }

        viewModel.onEvent(TransactionEntryEvent.SetTransactionType(EntryMode.TRANSFER))
        awaitState { it.isTransfer }
        
        // 1. Source (EUR) -> Outside (should be EUR)
        viewModel.onEvent(TransactionEntryEvent.SetAccount(mockToAccount)) // acc2 is EUR
        awaitState { state -> (state.source.account as? AccountRef.Wallet)?.id == "acc2" }
        
        val state1 = awaitState { it.destination.account.isOutside && it.destination.currency == "EUR" }
        assertEquals("EUR", state1.destination.currency)
        
        // 2. Change Source to USD -> Target (Outside) should follow to USD
        viewModel.onEvent(TransactionEntryEvent.SetAccount(mockAccount)) // acc1 is USD
        val state2 = awaitState { state -> (state.source.account as? AccountRef.Wallet)?.id == "acc1" && state.source.currency == "USD" }
        assertEquals("USD", state2.destination.currency)

        // 3. Manually change Source currency to EUR -> Target (Outside) should follow
        viewModel.onEvent(TransactionEntryEvent.SetCurrency("EUR"))
        val state3 = awaitState { it.source.currency == "EUR" }
        assertEquals("EUR", state3.destination.currency)
    }

    @Test
    fun `same source and target currency results in 1 to 1 rate`() = runTest {
        backgroundScope.launch { viewModel.state.collect {} }
        awaitState { it.accounts.isNotEmpty() }

        viewModel.onEvent(TransactionEntryEvent.SetTransactionType(EntryMode.TRANSFER))
        awaitState { it.isTransfer }
        
        viewModel.onEvent(TransactionEntryEvent.SetAccount(mockAccount))
        awaitState { (it.source.account as? AccountRef.Wallet)?.id == "acc1" }
        
        viewModel.onEvent(TransactionEntryEvent.SetAmount("100"))
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
    fun `swapping accounts in auto mode correctly swaps values and recalculates target`() = runTest {
        backgroundScope.launch { viewModel.state.collect {} }
        awaitState { it.accounts.isNotEmpty() }

        viewModel.onEvent(TransactionEntryEvent.SetTransactionType(EntryMode.TRANSFER))
        awaitState { it.isTransfer }
        
        viewModel.onEvent(TransactionEntryEvent.SetAccount(mockAccount)) // USD
        viewModel.onEvent(TransactionEntryEvent.SetTargetAccount(mockToAccount)) // EUR (0.85)
        awaitState { state -> 
            (state.source.account as? AccountRef.Wallet)?.id == "acc1" && 
            (state.destination.account as? AccountRef.Wallet)?.id == "acc2" 
        }
        
        viewModel.onEvent(TransactionEntryEvent.SetAmount("100"))
        awaitState { it.amount.toBD().compareTo(BigDecimal("100")) == 0 && it.targetAmount.toBD().compareTo(BigDecimal("85")) == 0 }
        
        viewModel.onEvent(TransactionEntryEvent.SwapAccounts)
        
        val state = awaitState { (it.source.account as? AccountRef.Wallet)?.id == "acc2" && it.amount.toBD().compareTo(BigDecimal("85")) == 0 }
        
        assertEquals("acc2", (state.source.account as? AccountRef.Wallet)?.id)
        assertEquals("EUR", state.selectedCurrency)
        assertEquals("acc1", (state.destination.account as? AccountRef.Wallet)?.id)
        assertEquals("USD", state.targetCurrency)
        
        val finalState = awaitState { it.targetAmount.toBD().compareTo(BigDecimal("100")) == 0 }
        assertEquals(0, BigDecimal("100").compareTo(finalState.targetAmount.toBD()))
    }

    @Test
    fun `SetAmount updates target and SetTargetAmount updates source`() = runTest {
        backgroundScope.launch { viewModel.state.collect {} }
        awaitState { it.accounts.isNotEmpty() }

        viewModel.onEvent(TransactionEntryEvent.SetTransactionType(EntryMode.TRANSFER))
        awaitState { it.isTransfer }
        
        viewModel.onEvent(TransactionEntryEvent.SetAccount(mockAccount)) // USD
        viewModel.onEvent(TransactionEntryEvent.SetTargetAccount(mockToAccount)) // EUR (0.85)
        awaitState { (it.source.account as? AccountRef.Wallet)?.id == "acc1" && (it.destination.account as? AccountRef.Wallet)?.id == "acc2" }
        
        viewModel.onEvent(TransactionEntryEvent.SetAmount("100"))
        val state1 = awaitState { it.targetAmount.toBD().compareTo(BigDecimal("85")) == 0 }
        assertEquals(0, BigDecimal("85").compareTo(state1.targetAmount.toBD()))
        
        viewModel.onEvent(TransactionEntryEvent.SetTargetAmount("170"))
        val state2 = awaitState { it.amount.toBD().compareTo(BigDecimal("200")) == 0 }
        assertEquals(0, BigDecimal("200").compareTo(state2.amount.toBD()))
    }
}
