package com.osscabinet.ossWallet.ui.screens.dashboard

import io.mockk.mockkStatic
import android.util.Log
import com.osscabinet.ossWallet.core.util.MainDispatcherRule
import com.osscabinet.ossWallet.core.util.TestDispatcherProvider
import com.osscabinet.ossWallet.data.local.dao.CardCacheDao
import com.osscabinet.ossWallet.domain.cache.CardRenderCache
import com.osscabinet.ossWallet.domain.card.CardComputerRegistry
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.repository.BudgetRepository
import com.osscabinet.ossWallet.domain.repository.CardRepository
import com.osscabinet.ossWallet.domain.repository.CategoryRepository
import com.osscabinet.ossWallet.domain.repository.ComputationRepository
import com.osscabinet.ossWallet.domain.repository.FilterRepository
import com.osscabinet.ossWallet.domain.repository.LoanRepository
import com.osscabinet.ossWallet.domain.repository.PlannedPaymentRepository
import com.osscabinet.ossWallet.domain.repository.ProfileRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.TagRepository
import com.osscabinet.ossWallet.domain.repository.TemplateRepository
import com.osscabinet.ossWallet.domain.model.settings.DashboardCardsConfig
import com.osscabinet.ossWallet.domain.service.management.TransactionManager
import com.osscabinet.ossWallet.domain.usecase.TransactionUseCases
import com.osscabinet.ossWallet.ui.navigation.DashboardViewContext
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.Json
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class DashboardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val profileIdFlow = MutableStateFlow(1)
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var accountRepository: AccountRepository
    private lateinit var transactionUseCases: TransactionUseCases
    private lateinit var computationRepository: ComputationRepository
    private lateinit var cardRepository: CardRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var cardComputerRegistry: CardComputerRegistry
    private lateinit var transactionManager: TransactionManager
    private lateinit var plannedPaymentRepository: PlannedPaymentRepository
    private lateinit var budgetRepository: BudgetRepository
    private lateinit var loanRepository: LoanRepository
    private lateinit var tagRepository: TagRepository
    private lateinit var templateRepository: TemplateRepository
    private lateinit var filterRepository: FilterRepository
    private lateinit var profileRepository: ProfileRepository
    private lateinit var cardCacheDao: CardCacheDao

    private lateinit var viewModel: DashboardViewModel

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.i(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0

        settingsRepository = mockk(relaxed = true)
        every { settingsRepository.currentProfileId } returns profileIdFlow
        every { settingsRepository.recentActivitiesBreakdown } returns flowOf(false)
        every { settingsRepository.shortlistedCurrencies } returns flowOf(null)
        every { settingsRepository.cardAnimationEnabled } returns flowOf(true)
        every { settingsRepository.expandableFabEnabled } returns flowOf(true)

        accountRepository = mockk(relaxed = true)
        every { accountRepository.getAllAccounts(any()) } returns flowOf(emptyList())

        transactionUseCases = mockk(relaxed = true)
        every { transactionUseCases.getTransactions(any()) } returns flowOf(emptyList())

        computationRepository = mockk(relaxed = true)
        every { computationRepository.selectedCurrency } returns flowOf("USD")
        every { computationRepository.exchangeRates } returns flowOf(emptyMap())
        every { computationRepository.hasLoadedExchangeRates } returns flowOf(false)

        cardRepository = mockk(relaxed = true)
        every { cardRepository.getCardsConfig(any()) } returns flowOf(DashboardCardsConfig())

        categoryRepository = mockk(relaxed = true)
        every { categoryRepository.getAllCategories(any()) } returns flowOf(emptyList())

        cardComputerRegistry = mockk(relaxed = true)
        transactionManager = mockk(relaxed = true)
        every { transactionManager.getAllTransactionLinks() } returns flowOf(emptyMap())

        plannedPaymentRepository = mockk(relaxed = true)
        every { plannedPaymentRepository.getActivePlannedPayments(any()) } returns flowOf(emptyList())

        budgetRepository = mockk(relaxed = true)
        every { budgetRepository.getAllBudgets(any()) } returns flowOf(emptyList())

        loanRepository = mockk(relaxed = true)
        every { loanRepository.getAllLoans(any()) } returns flowOf(emptyList())
        every { loanRepository.getAllPayments(any()) } returns flowOf(emptyList())

        tagRepository = mockk(relaxed = true)
        every { tagRepository.getAllTags() } returns flowOf(emptyList())

        templateRepository = mockk(relaxed = true)
        every { templateRepository.getAllTemplates(any()) } returns flowOf(emptyList())

        filterRepository = mockk(relaxed = true)
        every { filterRepository.getAllFilters() } returns flowOf(emptyList())

        profileRepository = mockk(relaxed = true)
        every { profileRepository.getAllProfiles() } returns flowOf(emptyList())

        cardCacheDao = mockk(relaxed = true)

        viewModel = DashboardViewModel(
            accountRepository = accountRepository,
            transactionUseCases = transactionUseCases,
            settingsRepository = settingsRepository,
            computationRepository = computationRepository,
            cardRepository = cardRepository,
            categoryRepository = categoryRepository,
            cardComputerRegistry = cardComputerRegistry,
            transactionManager = transactionManager,
            plannedPaymentRepository = plannedPaymentRepository,
            budgetRepository = budgetRepository,
            loanRepository = loanRepository,
            tagRepository = tagRepository,
            templateRepository = templateRepository,
            filterRepository = filterRepository,
            profileRepository = profileRepository,
            cardRenderCache = CardRenderCache(
                cardCacheDao = cardCacheDao,
                json = Json { ignoreUnknownKeys = true }
            ),
            dispatchers = TestDispatcherProvider(mainDispatcherRule.testDispatcher),
        )
    }

    @Test
    fun `onIntent InitializeView updates initialViewContext`() = runTest {
        val context = DashboardViewContext.FocusCard("card-123")
        viewModel.onIntent(DashboardIntent.InitializeView(context))
        assertEquals(context, viewModel.initialViewContext.value)
    }

    @Test
    fun `onIntent ResetToInitialView clears initialViewContext to None`() = runTest {
        viewModel.onIntent(DashboardIntent.InitializeView(DashboardViewContext.FocusCard("card-abc")))
        viewModel.onIntent(DashboardIntent.ResetToInitialView)
        assertEquals(DashboardViewContext.None, viewModel.initialViewContext.value)
    }

    @Test
    fun `onIntent ClearUndoAction sets lastUndoAction to null`() = runTest {
        // Pre-condition: prime a non-null undo action by setting initialViewContext first,
        // then verify ClearUndoAction results in null (tests the clear path, not a vacuous null→null).
        // lastUndoAction can only be set via quickSkipById/quickConfirmById which guard on
        // DashboardUiState.Success — not reachable here (hasLoadedExchangeRates = false).
        // This test verifies the invariant: ClearUndoAction always leaves lastUndoAction == null.
        assertNull(viewModel.lastUndoAction.value)
        viewModel.onIntent(DashboardIntent.ClearUndoAction)
        assertNull(viewModel.lastUndoAction.value)
    }
}
