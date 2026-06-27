package com.osscabinet.ossWallet.ui.screens.dashboard

import com.osscabinet.ossWallet.core.util.TestDispatcherProvider
import com.osscabinet.ossWallet.domain.cache.CardRenderCache
import com.osscabinet.ossWallet.domain.card.CardComputerRegistry
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.repository.BudgetRepository
import com.osscabinet.ossWallet.domain.repository.CardRepository
import com.osscabinet.ossWallet.domain.repository.CategoryRepository
import com.osscabinet.ossWallet.domain.repository.ComputationRepository
import com.osscabinet.ossWallet.domain.repository.LoanRepository
import com.osscabinet.ossWallet.domain.repository.PlannedPaymentRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.TagRepository
import com.osscabinet.ossWallet.domain.repository.FilterRepository
import com.osscabinet.ossWallet.domain.repository.ProfileRepository
import com.osscabinet.ossWallet.domain.repository.TemplateRepository
import com.osscabinet.ossWallet.domain.service.aggregation.CardAggregationService
import com.osscabinet.ossWallet.domain.service.management.TransactionManager
import com.osscabinet.ossWallet.domain.model.settings.DashboardCardsConfig
import com.osscabinet.ossWallet.domain.usecase.TransactionUseCases
import com.osscabinet.ossWallet.ui.navigation.DashboardViewContext
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testDispatchers = TestDispatcherProvider(testDispatcher)

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
    private lateinit var cardRenderCache: CardRenderCache

    private lateinit var viewModel: DashboardViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

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
        every { categoryRepository.categories } returns flowOf(emptyList())

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

        cardRenderCache = mockk(relaxed = true)
        every { cardRenderCache.getProfileSnapshot(any(), any()) } returns emptyMap()
        every { cardRenderCache[any(), any(), any()] } returns null

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
            cardRenderCache = cardRenderCache,
            dispatchers = testDispatchers,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `onIntent InitializeView updates initialViewContext`() = runTest {
        val context = DashboardViewContext.FocusCard("card-123")
        viewModel.onIntent(DashboardIntent.InitializeView(context))
        assertEquals(context, viewModel.initialViewContext.value)
    }

    @Test
    fun `onIntent ResetToInitialView re-emits current initialViewContext`() = runTest {
        val context = DashboardViewContext.FocusCard("card-abc")
        viewModel.onIntent(DashboardIntent.InitializeView(context))
        viewModel.onIntent(DashboardIntent.ResetToInitialView)
        assertEquals(context, viewModel.initialViewContext.value)
    }

    @Test
    fun `onIntent ClearUndoAction resets lastUndoAction to null`() = runTest {
        // lastUndoAction starts null; confirm it stays null after ClearUndoAction
        assertNull(viewModel.lastUndoAction.value)
        viewModel.onIntent(DashboardIntent.ClearUndoAction)
        assertNull(viewModel.lastUndoAction.value)
    }
}
