package com.osscabinet.ossWallet.assistant

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.osscabinet.ossWallet.MainActivity
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.EntryMode
import com.osscabinet.ossWallet.domain.model.PaymentMethod
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.model.dto.TransactionPrefillDTO
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.repository.CategoryRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Trampoline activity for Google Assistant App Actions and Bixby App Control.
 * Reads assistant-supplied parameters from the intent, resolves names to IDs,
 * then delegates to MainActivity with the serialized prefill and a nav target.
 * No UI — finishes immediately after launching MainActivity.
 */
@AndroidEntryPoint
class AssistantActivity : ComponentActivity() {

    @Inject lateinit var accountRepository: AccountRepository
    @Inject lateinit var categoryRepository: CategoryRepository
    @Inject lateinit var settingsRepository: SettingsRepository

    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + Dispatchers.Main)

    companion object {
        // Intent extras set by shortcuts.xml <parameter android:key="...">
        const val EXTRA_TRANSACTION_TYPE = "transaction_type"
        const val EXTRA_AMOUNT = "amount"
        const val EXTRA_ACCOUNT_NAME = "account_name"
        const val EXTRA_CATEGORY_NAME = "category_name"
        const val EXTRA_NOTE = "note"

        // Assistant nav targets (values for MainActivity.EXTRA_ASSISTANT_NAV)
        const val NAV_CREATE_TRANSACTION = "create_transaction"
        const val NAV_ACCOUNTS = "accounts"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val action = intent?.action ?: ""
        val dataPath = intent?.data?.path ?: ""
        when {
            action.contains("RECORD_TRANSACTION", ignoreCase = true) ||
            action.contains("CREATE_TRANSACTION", ignoreCase = true) ||
            dataPath.contains("record-transaction", ignoreCase = true) ||
            dataPath.contains("create-transaction", ignoreCase = true) ->
                handleCreateTransaction()
            action.contains("GET_ACCOUNT_BALANCE", ignoreCase = true) ||
            action.contains("ACCOUNT_BALANCE", ignoreCase = true) ||
            dataPath.contains("account-balance", ignoreCase = true) ||
            dataPath.contains("balance", ignoreCase = true) ->
                openAccounts()
            else -> {
                // Shortcut fallback (no data URI, no special action): open create transaction
                // so the plain create_transaction shortcut intent lands on the right screen.
                val navTarget = intent?.getStringExtra(MainActivity.EXTRA_ASSISTANT_NAV)
                if (navTarget == NAV_ACCOUNTS) openAccounts()
                else handleCreateTransaction()
            }
        }
    }

    private fun handleCreateTransaction() {
        scope.launch {
            val profileId = settingsRepository.currentProfileId.first()
            val defaultCurrency = settingsRepository.currency.first()

            val typeRaw = intent?.getStringExtra(EXTRA_TRANSACTION_TYPE)
            val entryMode = when (typeRaw?.lowercase()) {
                "income", "credit" -> EntryMode.INCOME
                "transfer" -> EntryMode.TRANSFER
                else -> EntryMode.EXPENSE
            }

            val amountRaw = intent?.getStringExtra(EXTRA_AMOUNT)?.trim() ?: ""
            val accountName = intent?.getStringExtra(EXTRA_ACCOUNT_NAME)?.trim() ?: ""
            val categoryName = intent?.getStringExtra(EXTRA_CATEGORY_NAME)?.trim() ?: ""
            val note = intent?.getStringExtra(EXTRA_NOTE)?.trim() ?: ""

            val accounts = accountRepository.getAllAccounts(profileId).first()
            val account = accounts.firstOrNull { it.name.equals(accountName, ignoreCase = true) }

            val categories = categoryRepository.getAllCategories(profileId).first()
            val category = categories.firstOrNull { it.name.equals(categoryName, ignoreCase = true) }

            val dto = TransactionPrefillDTO(
                entryMode = entryMode,
                amount = amountRaw,
                accountId = account?.id,
                currency = account?.currencyCode ?: defaultCurrency,
                categoryId = category?.id ?: Category.SYSTEM_OTHERS,
                note = note,
                paymentMethod = PaymentMethod.CASH,
                status = TransactionStatus.CLEARED,
            )

            val prefillJson = runCatching { Json.encodeToString(dto) }.getOrNull()
            launchMainActivity(NAV_CREATE_TRANSACTION, prefillJson)
        }
    }

    private fun openAccounts() {
        launchMainActivity(NAV_ACCOUNTS, prefillJson = null)
    }

    private fun launchMainActivity(navTarget: String, prefillJson: String?) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_ASSISTANT_NAV, navTarget)
            if (prefillJson != null) putExtra(MainActivity.EXTRA_ASSISTANT_PREFILL, prefillJson)
        }
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
