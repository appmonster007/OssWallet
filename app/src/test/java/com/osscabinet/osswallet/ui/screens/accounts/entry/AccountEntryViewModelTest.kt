package com.osscabinet.ossWallet.ui.screens.accounts.entry

import com.osscabinet.ossWallet.core.util.MainDispatcherRule
import com.osscabinet.ossWallet.domain.service.query.DatabaseQueryService
import com.osscabinet.ossWallet.domain.usecase.AccountUseCases
import com.osscabinet.ossWallet.test.FakeAccountRepository
import com.osscabinet.ossWallet.test.FakeComputationRepository
import com.osscabinet.ossWallet.test.FakeSettingsRepository
import com.osscabinet.ossWallet.test.FakeTransactionRepository
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class AccountEntryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private lateinit var accountUseCases: AccountUseCases
    private lateinit var viewModel: AccountEntryViewModel

    @Before
    fun setup() {
        accountUseCases = mockk(relaxed = true)
        viewModel = AccountEntryViewModel(
            accountUseCases = accountUseCases,
            accountRepository = FakeAccountRepository(),
            transactionRepository = FakeTransactionRepository(),
            computationRepository = FakeComputationRepository(),
            queryService = mockk<DatabaseQueryService>(relaxed = true),
            settingsRepository = FakeSettingsRepository(initialProfileId = 1, initialCurrency = "USD")
        )
    }

    /** Opens the create form in [currency], the way a new account starts. */
    private fun TestScope.openCreateForm(currency: String = "USD") {
        backgroundScope.launch { viewModel.state.collect {} }
        viewModel.initialize(prefillCurrency = currency)
        advanceUntilIdle()
    }

    private fun TestScope.openBalanceSheet(currency: String = "USD") {
        viewModel.onEvent(AccountEntryEvent.OpenBalanceUpdate(currency))
        advanceUntilIdle()
    }

    // ── Issue 8: before the account exists, "By Record" is the opening balance ──

    @Test
    fun `create mode - recording a balance sets the opening balance, not an adjustment`() = runTest(testDispatcher) {
        openCreateForm()

        openBalanceSheet()
        assertEquals(BalanceUpdateMode.INITIAL_BALANCE, viewModel.state.value.balanceUpdateMode)
        viewModel.onEvent(AccountEntryEvent.SetBalanceUpdateAmount("750"))
        viewModel.onEvent(AccountEntryEvent.ConfirmBalanceUpdate)
        advanceUntilIdle()

        assertEquals("750", viewModel.state.value.initialBalances["USD"])
        assertTrue(viewModel.state.value.pendingAdjustmentDeltas.isEmpty())
    }

    @Test
    fun `create mode - recording again replaces the opening balance instead of adding to it`() = runTest(testDispatcher) {
        openCreateForm()
        openBalanceSheet()
        viewModel.onEvent(AccountEntryEvent.SetBalanceUpdateAmount("750"))
        viewModel.onEvent(AccountEntryEvent.ConfirmBalanceUpdate)
        advanceUntilIdle()

        openBalanceSheet()
        assertEquals("750", viewModel.state.value.balanceUpdateInputAmount)
        viewModel.onEvent(AccountEntryEvent.SetBalanceUpdateAmount("900"))
        viewModel.onEvent(AccountEntryEvent.ConfirmBalanceUpdate)
        advanceUntilIdle()

        assertEquals("900", viewModel.state.value.initialBalances["USD"])
    }

    @Test
    fun `create mode - saving writes the typed balance as the opening balance and no adjustments`() = runTest(testDispatcher) {
        openCreateForm()
        viewModel.onEvent(AccountEntryEvent.SetName("Cash"))
        openBalanceSheet()
        viewModel.onEvent(AccountEntryEvent.SetBalanceUpdateAmount("900"))
        viewModel.onEvent(AccountEntryEvent.ConfirmBalanceUpdate)
        advanceUntilIdle()

        viewModel.saveAccount {}
        advanceUntilIdle()

        coVerify(exactly = 1) {
            accountUseCases.createAccountWithInitialBalances(
                any(),
                match { it["USD"] == "900" },
                any(), any(), any(), any(), any(), any(), any(), any()
            )
        }
        coVerify(exactly = 0) { accountUseCases.createBalanceAdjustment(any(), any(), any(), any()) }
        coVerify(exactly = 0) { accountUseCases.saveAccountEdits(any(), any(), any(), any(), any()) }
    }

    // ── Issue 7: a second tap before the first save finishes must not repeat it ──

    @Test
    fun `a second save while the first is in flight is ignored`() = runTest(testDispatcher) {
        openCreateForm()
        viewModel.onEvent(AccountEntryEvent.SetName("Cash"))
        advanceUntilIdle()

        viewModel.saveAccount {}
        viewModel.saveAccount {}
        advanceUntilIdle()

        coVerify(exactly = 1) {
            accountUseCases.createAccountWithInitialBalances(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
            )
        }
    }

    @Test
    fun `saving is allowed again once the first save has finished`() = runTest(testDispatcher) {
        openCreateForm()
        viewModel.onEvent(AccountEntryEvent.SetName("Cash"))
        advanceUntilIdle()

        viewModel.saveAccount {}
        advanceUntilIdle()
        viewModel.saveAccount {}
        advanceUntilIdle()

        coVerify(exactly = 2) {
            accountUseCases.createAccountWithInitialBalances(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
            )
        }
    }
}
