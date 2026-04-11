package com.osscabinet.gleemanager

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material3.Button
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.osscabinet.gleemanager.data.local.AppDatabase
import com.osscabinet.gleemanager.data.repository.TransactionRepositoryImpl
import com.osscabinet.gleemanager.domain.usecase.*
import com.osscabinet.gleemanager.data.repository.BudgetRepositoryImpl
import com.osscabinet.gleemanager.ui.viewmodel.BudgetViewModel
import com.osscabinet.gleemanager.ui.viewmodel.BudgetViewModelFactory
import com.osscabinet.gleemanager.ui.screens.*
import com.osscabinet.gleemanager.ui.theme.GleeManagerTheme
import com.osscabinet.gleemanager.data.repository.SharedWalletRepositoryImpl
import com.osscabinet.gleemanager.ui.viewmodel.SharedWalletViewModel
import com.osscabinet.gleemanager.ui.viewmodel.AnalyticsViewModel
import com.osscabinet.gleemanager.ui.viewmodel.TransactionViewModel
import com.osscabinet.gleemanager.ui.viewmodel.TransactionViewModelFactory

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Manual Injection for now
        val db = AppDatabase.getDatabase(this, "secret_passphrase".toByteArray())
        val repository = TransactionRepositoryImpl(db.transactionDao())
        val getTransactionsUseCase = GetTransactionsUseCase(repository)
        val insertTransactionUseCase = InsertTransactionUseCase(repository)
        val deleteTransactionUseCase = DeleteTransactionUseCase(repository)
        val settingsDataStore = com.osscabinet.gleemanager.data.local.SettingsDataStore(this)
        val accountRepository = com.osscabinet.gleemanager.data.repository.AccountRepositoryImpl(db.accountDao())
        val updateTransactionUseCase = UpdateTransactionUseCase(repository, accountRepository)
        val viewModelFactory = TransactionViewModelFactory(getTransactionsUseCase, insertTransactionUseCase, deleteTransactionUseCase, updateTransactionUseCase, settingsDataStore)
        
        val budgetRepository = BudgetRepositoryImpl(db.budgetDao())
        val syncBudgetSpentUseCase = SyncBudgetSpentUseCase(budgetRepository, repository)
        val upsertBudgetUseCase = UpsertBudgetUseCase(budgetRepository)
        val budgetViewModelFactory = BudgetViewModelFactory(syncBudgetSpentUseCase, upsertBudgetUseCase)
        
        val currencyApi = com.osscabinet.gleemanager.data.remote.RetrofitClient.currencyApi
        val currencyRepository = com.osscabinet.gleemanager.data.repository.CurrencyRepositoryImpl(currencyApi, db.currencyDao())
        val getExchangeRatesUseCase = com.osscabinet.gleemanager.domain.usecase.GetExchangeRatesUseCase(currencyRepository)
        
        val analyticsViewModelFactory = com.osscabinet.gleemanager.ui.viewmodel.GenericViewModelFactory {
            AnalyticsViewModel(getTransactionsUseCase, getExchangeRatesUseCase)
        }
        
        val sharedWalletRepository = SharedWalletRepositoryImpl(db.transactionDao())
        val sharedWalletViewModelFactory = com.osscabinet.gleemanager.ui.viewmodel.GenericViewModelFactory {
            SharedWalletViewModel(sharedWalletRepository)
        }

        val loanRepository = com.osscabinet.gleemanager.data.repository.LoanRepositoryImpl(db.loanDao())
        val loanViewModelFactory = com.osscabinet.gleemanager.ui.viewmodel.LoanViewModelFactory(loanRepository)

        val profileRepository = com.osscabinet.gleemanager.data.repository.ProfileRepositoryImpl(db.profileDao(), settingsDataStore)
        val backupRepository = com.osscabinet.gleemanager.data.repository.BackupRepositoryImpl(contentResolver, db.profileDao(), db.accountDao(), db.transactionDao(), db.categoryDao(), db.tagDao())
        val settingsViewModelFactory = com.osscabinet.gleemanager.ui.viewmodel.SettingsViewModelFactory(settingsDataStore, profileRepository, backupRepository)
        val accountViewModelFactory = com.osscabinet.gleemanager.ui.viewmodel.AccountViewModelFactory(accountRepository, settingsDataStore)

        setContent {
            val theme by settingsDataStore.theme.collectAsState(initial = "System")
            GleeManagerTheme(theme = theme) {
                val navController = rememberNavController()
                val viewModel: TransactionViewModel = viewModel(factory = viewModelFactory)
                val budgetViewModel: BudgetViewModel = viewModel(factory = budgetViewModelFactory)
                val loanViewModel: com.osscabinet.gleemanager.ui.viewmodel.LoanViewModel = viewModel(factory = loanViewModelFactory)
                val settingsViewModel: com.osscabinet.gleemanager.ui.viewmodel.SettingsViewModel = viewModel(factory = settingsViewModelFactory)
                val accountViewModel: com.osscabinet.gleemanager.ui.viewmodel.AccountViewModel = viewModel(factory = accountViewModelFactory)
                val analyticsViewModel: AnalyticsViewModel = viewModel(factory = analyticsViewModelFactory)
                val sharedWalletViewModel: SharedWalletViewModel = viewModel(factory = sharedWalletViewModelFactory)

                NavHost(navController = navController, startDestination = "dashboard") {
                    composable("dashboard") {
                        DashboardScreen(
                            onAnalyticsClick = { navController.navigate("analytics") },
                            onRecordsClick = { navController.navigate("list") },
                            onBudgetsClick = { navController.navigate("budgets") },
                            onAddClick = { navController.navigate("add") },
                            onManageAccountsClick = { navController.navigate("manage_accounts") },
                            onAccountClick = { accountId -> navController.navigate("account_detail/$accountId") },
                            onLoansClick = { navController.navigate("loans") },
                            onSettingsClick = { navController.navigate("settings") },
                            onAccountsViewClick = { navController.navigate("accounts_view") },
                            accountViewModel = accountViewModel,
                            transactionViewModel = viewModel,
                            settingsViewModel = settingsViewModel,
                            analyticsViewModel = analyticsViewModel
                        )
                    }
                    composable("accounts_view") {
                        AccountsViewScreen(
                            onDashboardClick = { navController.navigate("dashboard") { popUpTo("dashboard") { inclusive = true } } },
                            onRecordsClick = { navController.navigate("list") },
                            onAnalyticsClick = { navController.navigate("analytics") },
                            onBudgetsClick = { navController.navigate("budgets") },
                            onLoansClick = { navController.navigate("loans") },
                            onManageAccountsClick = { navController.navigate("manage_accounts") },
                            onSettingsClick = { navController.navigate("settings") },
                            onAccountClick = { accountId -> navController.navigate("account_detail/$accountId") },
                            onAddAccountClick = { navController.navigate("account_setup") },
                            accountViewModel = accountViewModel,
                            transactionViewModel = viewModel,
                            settingsViewModel = settingsViewModel,
                            analyticsViewModel = analyticsViewModel
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            viewModel = settingsViewModel,
                            onManageAccountsClick = { navController.navigate("manage_accounts") },
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                    composable("loans") {
                        LoansScreen(
                            viewModel = loanViewModel,
                            settingsViewModel = settingsViewModel,
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                    composable("budgets") {
                        BudgetsScreen(
                            viewModel = budgetViewModel,
                            settingsViewModel = settingsViewModel,
                            onBackClick = { navController.popBackStack() },
                            onEditBudgetClick = { category -> navController.navigate("edit_budget/$category") }
                        )
                    }
                    composable("edit_budget/{category}") { backStackEntry ->
                        val category = backStackEntry.arguments?.getString("category") ?: ""
                        EditBudgetScreen(
                            category = category,
                            viewModel = budgetViewModel,
                            settingsViewModel = settingsViewModel,
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                    composable("list") {
                        TransactionListScreen(
                            viewModel = viewModel,
                            accountViewModel = accountViewModel,
                            onAddTransactionClick = { navController.navigate("add") },
                            onEditTransactionClick = { transactionId -> navController.navigate("edit_transaction/$transactionId") },
                            onBackClick = { navController.popBackStack() },
                            onAccountDetailClick = { accountId -> navController.navigate("account_detail/$accountId") }
                        )
                    }
                    composable("add") {
                        AddTransactionScreen(
                            viewModel = viewModel,
                            accountViewModel = accountViewModel,
                            analyticsViewModel = analyticsViewModel,
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                    composable("edit_transaction/{transactionId}") { backStackEntry ->
                        val transactionId = backStackEntry.arguments?.getString("transactionId")?.toLongOrNull() ?: -1L
                        EditTransactionScreen(
                            transactionId = transactionId,
                            viewModel = viewModel,
                            accountViewModel = accountViewModel,
                            analyticsViewModel = analyticsViewModel,
                            settingsViewModel = settingsViewModel,
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                    composable("account_detail/{accountId}") { backStackEntry ->
                        val accountId = backStackEntry.arguments?.getString("accountId") ?: ""
                        AccountDetailScreen(
                            accountId = accountId,
                            accountViewModel = accountViewModel,
                            transactionViewModel = viewModel,
                            settingsViewModel = settingsViewModel,
                            analyticsViewModel = analyticsViewModel,
                            onBackClick = { navController.popBackStack() },
                            onEditAccountClick = { navController.navigate("edit_account/$accountId") },
                            onEditTransactionClick = { transactionId -> navController.navigate("edit_transaction/$transactionId") }
                        )
                    }
                    composable("edit_account/{accountId}") { backStackEntry ->
                        val accountId = backStackEntry.arguments?.getString("accountId") ?: ""
                        EditAccountScreen(
                            accountId = accountId,
                            viewModel = accountViewModel,
                            transactionViewModel = viewModel,
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                    composable("account_setup") {
                        AccountSetupScreen(
                            onBackClick = { navController.popBackStack() },
                            onAccountCreated = { navController.popBackStack() },
                            viewModel = accountViewModel
                        )
                    }
                    composable("manage_accounts") {
                        AccountManagementScreen(
                            onBackClick = { navController.popBackStack() },
                            onAddAccountClick = { navController.navigate("account_setup") },
                            viewModel = accountViewModel
                        )
                    }
                    composable("analytics") {
                        AnalyticsScreen(
                            viewModel = analyticsViewModel,
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                    composable("shared") {
                        SharedWalletScreen(
                            viewModel = sharedWalletViewModel,
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    GleeManagerTheme {
        Greeting("GleeManager User")
    }
}
