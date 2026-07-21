package com.osscabinet.ossWallet.ui.screens.dashboard.quickadd

import com.osscabinet.ossWallet.core.util.MainDispatcherRule
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.AccountRef
import com.osscabinet.ossWallet.domain.model.AccountStructureType
import com.osscabinet.ossWallet.domain.model.AmountEditingSide
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.EntryMode
import com.osscabinet.ossWallet.domain.model.Template
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.domain.service.management.TransactionManager
import com.osscabinet.ossWallet.domain.service.query.DatabaseQueryService
import com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot
import com.osscabinet.ossWallet.test.FakeAccountRepository
import com.osscabinet.ossWallet.test.FakeCategoryRepository
import com.osscabinet.ossWallet.test.FakeComputationRepository
import com.osscabinet.ossWallet.test.FakeSettingsRepository
import com.osscabinet.ossWallet.test.FakeTemplateRepository
import com.osscabinet.ossWallet.test.FakeTransactionRepository
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class QuickAddViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

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

    private lateinit var fakeAccounts: FakeAccountRepository
    private lateinit var viewModel: QuickAddViewModel

    @Before
    fun setup() {
        CurrencyService.setFormatConfig(2, CurrencyService.FormattingSystem.WESTERN)

        fakeAccounts = FakeAccountRepository(listOf(mockAccount, mockToAccount))

        val mockQueryService = mockk<DatabaseQueryService>(relaxed = true)
        io.mockk.coEvery {
            mockQueryService.aggregate(
                any(), any(), any(), any(), any(), any(), any()
            )
        } returns Result.success(emptyList())

        viewModel = QuickAddViewModel(
            settingsRepository = FakeSettingsRepository(initialProfileId = 1, initialCurrency = "USD"),
            accountRepository = fakeAccounts,
            transactionManager = mockk(relaxed = true),
            transactionRepository = FakeTransactionRepository(),
            computationRepository = FakeComputationRepository(),
            queryService = mockQueryService,
            templateRepository = FakeTemplateRepository(),
            categoryRepository = FakeCategoryRepository()
        )
    }

    private suspend fun awaitState(predicate: (QuickAddState) -> Boolean): QuickAddState {
        return viewModel.state.filter(predicate).first()
    }

    @Test
    fun `initial state is correct`() = runTest {
        backgroundScope.launch { viewModel.state.collect {} }
        val state = awaitState { it.accounts.isNotEmpty() }
        assertEquals(EntryMode.EXPENSE, state.entryMode)
        assertEquals(BigDecimal.ZERO, state.amount)
        assertEquals("USD", state.baseCurrency)
    }

    @Test
    fun `toggleTransactionType cycles between expense and income only`() = runTest {
        backgroundScope.launch { viewModel.state.collect {} }
        awaitState { it.accounts.isNotEmpty() }

        viewModel.toggleTransactionType()
        awaitState { it.entryMode == EntryMode.INCOME }

        viewModel.toggleTransactionType()
        awaitState { it.entryMode == EntryMode.EXPENSE }

        viewModel.toggleTransactionType()
        awaitState { it.entryMode == EntryMode.INCOME }
    }

    @Test
    fun `toggleTransferMode switches between Expense and Transfer`() = runTest {
        backgroundScope.launch { viewModel.state.collect {} }
        awaitState { it.accounts.isNotEmpty() }

        viewModel.toggleTransferMode()
        awaitState { it.entryMode == EntryMode.TRANSFER }

        viewModel.toggleTransferMode()
        awaitState { it.entryMode == EntryMode.EXPENSE }
    }

    @Test
    fun `onAccountTapped updates source account and currency in Standard mode`() = runTest {
        backgroundScope.launch { viewModel.state.collect {} }
        awaitState { it.accounts.isNotEmpty() }

        viewModel.onAccountTapped("acc2", isFrom = true)
        val state = awaitState { (it.selectedAccount as? AccountRef.Wallet)?.id == "acc2" }
        assertEquals("EUR", state.selectedCurrency)
        assertEquals(2, state.currentStep)
    }

    @Test
    fun `onAccountTapped updates target account in Transfer mode and resolves currency`() = runTest {
        backgroundScope.launch { viewModel.state.collect {} }
        awaitState { it.accounts.isNotEmpty() }

        viewModel.toggleTransferMode()
        awaitState { it.entryMode == EntryMode.TRANSFER }

        viewModel.onAccountTapped("acc1", isFrom = true)
        awaitState { (it.selectedAccount as? AccountRef.Wallet)?.id == "acc1" }

        viewModel.onAccountTapped("acc2", isFrom = false)
        val state = awaitState { (it.selectedToAccount as? AccountRef.Wallet)?.id == "acc2" }
        assertEquals("EUR", state.selectedToCurrency)
    }

    @Test
    fun `outside wallet inherits currency from other side and enforces sync`() = runTest {
        backgroundScope.launch { viewModel.state.collect {} }
        awaitState { it.accounts.isNotEmpty() }

        viewModel.toggleTransferMode()
        awaitState { it.entryMode == EntryMode.TRANSFER }

        // 1. Source (EUR) -> Outside (should be EUR)
        viewModel.onAccountTapped("acc2", isFrom = true)
        awaitState { (it.selectedAccount as? AccountRef.Wallet)?.id == "acc2" && it.selectedCurrency == "EUR" }

        viewModel.onAccountTapped("", isFrom = false)
        val state1 = awaitState { it.selectedToAccount is AccountRef.Outside && it.selectedToCurrency == "EUR" }
        assertEquals("EUR", state1.selectedToCurrency)

        // 2. Change Source to USD -> Target (Outside) should follow to USD
        viewModel.onAccountTapped("acc1", isFrom = true)
        val state2 = awaitState { (it.selectedAccount as? AccountRef.Wallet)?.id == "acc1" && it.selectedCurrency == "USD" }
        assertEquals("USD", state2.selectedToCurrency)

        // 3. Manually change Source currency to EUR -> Target (Outside) should follow
        viewModel.onCurrencySelected("EUR", isFrom = true)
        val state3 = awaitState { it.selectedCurrency == "EUR" }
        assertEquals("EUR", state3.selectedToCurrency)
    }

    @Test
    fun `same source and target currency results in 1 to 1 rate`() = runTest {
        backgroundScope.launch { viewModel.state.collect {} }
        awaitState { it.accounts.isNotEmpty() && it.selectedCurrency == "USD" }

        viewModel.toggleTransferMode()
        awaitState { it.entryMode == EntryMode.TRANSFER }

        viewModel.onAccountTapped("acc1", isFrom = true)
        awaitState { (it.selectedAccount as? AccountRef.Wallet)?.id == "acc1" && it.selectedCurrency == "USD" }

        viewModel.onAccountTapped("", isFrom = false)
        awaitState { it.selectedToAccount is AccountRef.Outside && it.selectedToCurrency == "USD" }

        viewModel.onAmountChanged(BigDecimal("100"))
        val state = awaitState {
            it.amount.compareTo(BigDecimal("100")) == 0 &&
            it.selectedCurrency == "USD" && it.selectedToCurrency == "USD" &&
            it.effectiveTargetAmount.compareTo(BigDecimal("100")) == 0
        }

        assertEquals(0, BigDecimal("100").compareTo(state.effectiveTargetAmount))
    }

    @Test
    fun `onAmountChanged updates source amount and triggers target conversion in auto mode`() = runTest {
        backgroundScope.launch { viewModel.state.collect {} }
        awaitState { it.accounts.isNotEmpty() && it.exchangeRates.rates.containsKey("EUR") }

        viewModel.toggleTransferMode()
        awaitState { it.entryMode == EntryMode.TRANSFER }

        viewModel.onAccountTapped("acc1", isFrom = true) // USD
        awaitState { (it.selectedAccount as? AccountRef.Wallet)?.id == "acc1" }

        viewModel.onAccountTapped("acc2", isFrom = false) // EUR
        awaitState { (it.selectedToAccount as? AccountRef.Wallet)?.id == "acc2" }

        viewModel.onAmountChanged(BigDecimal("100"))
        val state = awaitState { it.amount.compareTo(BigDecimal("100")) == 0 }

        assertEquals(0, BigDecimal("85").compareTo(state.effectiveTargetAmount))
    }

    @Test
    fun `when isAmountOverridden is true, updating source does not change target and recalculates implied rate`() = runTest {
        backgroundScope.launch { viewModel.state.collect {} }
        awaitState { it.accounts.isNotEmpty() && it.exchangeRates.rates.containsKey("EUR") }

        viewModel.toggleTransferMode()
        viewModel.onAccountTapped("acc1", isFrom = true) // USD
        viewModel.onAccountTapped("acc2", isFrom = false) // EUR (Rate 0.85)
        viewModel.onAmountChanged(BigDecimal("100"))

        // Enable Fixed Mode
        viewModel.toggleAmountOverride()
        awaitState { it.isAmountOverridden }

        // 1. Change Source to 200 USD. Target (85 EUR) should remain UNCHANGED.
        viewModel.setAmountFocus(AmountEditingSide.SOURCE)
        viewModel.onAmountChanged(BigDecimal("200"))

        val state1 = awaitState { it.amount.compareTo(BigDecimal("200")) == 0 }
        assertEquals(0, BigDecimal("85").compareTo(state1.amountOverride ?: BigDecimal.ZERO))

        // 2. Change Target to 100 EUR. Source (200 USD) should remain UNCHANGED.
        viewModel.setAmountFocus(AmountEditingSide.TARGET)
        viewModel.onAmountChanged(BigDecimal("100"))

        val state2 = awaitState { it.amountOverride?.compareTo(BigDecimal("100")) == 0 }
        assertEquals(0, BigDecimal("200").compareTo(state2.amount))
    }

    @Test
    fun `setAmountFocus enables manual override when focusing target`() = runTest {
        backgroundScope.launch { viewModel.state.collect {} }
        awaitState { it.accounts.isNotEmpty() && it.exchangeRates.rates.containsKey("EUR") }

        viewModel.toggleTransferMode()
        awaitState { it.entryMode == EntryMode.TRANSFER }

        viewModel.onAccountTapped("acc1", isFrom = true)
        viewModel.onAccountTapped("acc2", isFrom = false)
        viewModel.onAmountChanged(BigDecimal("100"))

        viewModel.setAmountFocus(AmountEditingSide.TARGET)
        val state = awaitState { it.activeAmountSide == AmountEditingSide.TARGET }
        assertTrue("Focusing target side should enable amount override", state.isAmountOverridden)
        assertEquals(0, BigDecimal("85").compareTo(state.amountOverride ?: BigDecimal.ZERO))
    }

    @Test
    fun `toggleAmountOverride syncs values correctly`() = runTest {
        backgroundScope.launch { viewModel.state.collect {} }
        awaitState { it.accounts.isNotEmpty() && it.exchangeRates.rates.containsKey("EUR") }

        viewModel.toggleTransferMode()
        awaitState { it.entryMode == EntryMode.TRANSFER }

        viewModel.onAccountTapped("acc1", isFrom = true)
        viewModel.onAccountTapped("acc2", isFrom = false)
        viewModel.onAmountChanged(BigDecimal("100"))

        // Enable
        viewModel.toggleAmountOverride()
        val stateOn = awaitState { it.isAmountOverridden }
        assertEquals(0, BigDecimal("85").compareTo(stateOn.amountOverride ?: BigDecimal.ZERO))

        // Disable
        viewModel.toggleAmountOverride()
        val stateOff = awaitState { !it.isAmountOverridden }
        assertNull(stateOff.amountOverride)
    }

    @Test
    fun `onSwapAccounts swaps legs and correctly converts amount and rates`() = runTest {
        backgroundScope.launch { viewModel.state.collect {} }
        awaitState { it.accounts.isNotEmpty() && it.exchangeRates.rates.containsKey("EUR") }

        viewModel.toggleTransferMode()
        awaitState { it.entryMode == EntryMode.TRANSFER }

        viewModel.onAccountTapped("acc1", isFrom = true) // USD
        viewModel.onAccountTapped("acc2", isFrom = false) // EUR
        viewModel.onAmountChanged(BigDecimal("100"))

        awaitState { it.effectiveTargetAmount.compareTo(BigDecimal("85")) == 0 }

        viewModel.onSwapAccounts()
        val state = awaitState { s ->
            (s.selectedAccount as? AccountRef.Wallet)?.id == "acc2" &&
            s.amount.compareTo(BigDecimal("85")) == 0 &&
            s.selectedCurrency == "EUR" && s.selectedToCurrency == "USD"
        }

        assertEquals(0, BigDecimal("100").compareTo(state.effectiveTargetAmount))
    }

    @Test
    fun `focus guard pushes focus to Target when Source becomes Outside`() = runTest {
        backgroundScope.launch { viewModel.state.collect {} }
        awaitState { it.accounts.isNotEmpty() }

        viewModel.toggleTransferMode()
        awaitState { it.entryMode == EntryMode.TRANSFER }

        viewModel.onAccountTapped("acc1", isFrom = true)
        viewModel.setAmountFocus(AmountEditingSide.SOURCE)
        awaitState { it.activeAmountSide == AmountEditingSide.SOURCE }

        viewModel.onAccountTapped("", isFrom = true) // Change source to Outside
        val state = awaitState { it.selectedAccount is AccountRef.Outside }
        assertEquals(AmountEditingSide.TARGET, state.activeAmountSide)
    }

    @Test
    fun `focus guard pushes focus to Source when Target becomes Outside`() = runTest {
        backgroundScope.launch { viewModel.state.collect {} }
        awaitState { it.accounts.isNotEmpty() }

        viewModel.toggleTransferMode()
        awaitState { it.entryMode == EntryMode.TRANSFER }

        viewModel.onAccountTapped("acc2", isFrom = false)
        viewModel.setAmountFocus(AmountEditingSide.TARGET)
        awaitState { it.activeAmountSide == AmountEditingSide.TARGET }

        viewModel.onAccountTapped("", isFrom = false) // Change target to Outside
        val state = awaitState { it.selectedToAccount is AccountRef.Outside }
        assertEquals(AmountEditingSide.SOURCE, state.activeAmountSide)
    }

    @Test
    fun `canSave logic validates amount and accounts correctly`() = runTest {
        backgroundScope.launch { viewModel.state.collect {} }
        awaitState { it.accounts.isNotEmpty() }

        // Select a category to satisfy validation
        viewModel.onCategorySelected(Category(1, "Test", "Icon", 0, false, null))

        // 1. Standard - No category -> Invalid
        viewModel.onAmountChanged(BigDecimal.ZERO)
        assertFalse(viewModel.state.value.canSave)

        // Select a category to satisfy validation
        viewModel.onCategorySelected(Category(1, "Test", "Icon", 0, false, null))

        // 1b. Standard - Zero amount WITH valid account and category -> Valid
        viewModel.onAccountTapped("acc1", isFrom = true)
        assertTrue(awaitState { it.selectedAccount is AccountRef.Wallet }.canSave)

        // 2. Standard - Valid account and amount -> Valid
        viewModel.onAmountChanged(BigDecimal("10"))
        assertTrue(awaitState { it.canSave }.canSave)

        // 3. Transfer - Both Outside -> Invalid
        viewModel.toggleTransferMode()
        awaitState { it.entryMode == EntryMode.TRANSFER }
        viewModel.updateAccount("", isFrom = true)
        viewModel.updateAccount("", isFrom = false)
        assertFalse(awaitState { it.selectedAccount is AccountRef.Outside && it.selectedToAccount is AccountRef.Outside }.canSave)

        // 4. Transfer - Wallet to Outside -> Valid
        viewModel.updateAccount("acc1", isFrom = true)
        assertTrue(awaitState { (it.selectedAccount as? AccountRef.Wallet)?.id == "acc1" && it.selectedToAccount is AccountRef.Outside }.canSave)
    }

    @Test
    fun `onTemplateSelected prefills state correctly`() = runTest {
        backgroundScope.launch { viewModel.state.collect {} }
        awaitState { it.accounts.isNotEmpty() }

        val template = Template(
            id = 10,
            profileId = 1,
            name = "Test Temp",
            amount = BigDecimal("50"),
            currency = "EUR",
            accountId = "acc2",
            categoryId = 5,
            entryMode = EntryMode.EXPENSE,
            isStarred = false
        )

        viewModel.onTemplateSelected(template)
        val state = awaitState { it.selectedTemplate?.id == 10L }

        assertEquals(BigDecimal("50"), state.amount)
        assertEquals("EUR", state.selectedCurrency)
        assertEquals("acc2", (state.selectedAccount as? AccountRef.Wallet)?.id)
        assertEquals(5L, state.selectedCategoryId)
        assertEquals(2, state.currentStep)
    }

    @Test
    fun `saveTransaction invokes manager and handles success`() = runTest {
        backgroundScope.launch { viewModel.state.collect {} }
        awaitState { it.accounts.isNotEmpty() }

        viewModel.onAmountChanged(BigDecimal("10"))
        viewModel.onAccountTapped("acc1", isFrom = true)
        awaitState { it.canSave }

        var successCalled = false
        viewModel.saveTransaction { successCalled = true }

        awaitState { !it.isSaving }
        assertTrue(successCalled)
    }

    @Test
    fun `reset clears input but preserves accounts`() = runTest {
        backgroundScope.launch { viewModel.state.collect {} }
        awaitState { it.accounts.isNotEmpty() }

        viewModel.onAmountChanged(BigDecimal("100"))
        viewModel.toggleTransferMode()
        awaitState { it.entryMode == EntryMode.TRANSFER }

        viewModel.reset()
        val state = awaitState { it.amount == BigDecimal.ZERO }

        assertEquals(EntryMode.EXPENSE, state.entryMode)
        assertEquals(BigDecimal.ZERO, state.amount)
        assertTrue(state.accounts.isNotEmpty())
    }
}
