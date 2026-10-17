package com.osscabinet.ossWallet.appfunctions

import android.content.Context
import android.os.Build
import androidx.appfunctions.AppFunctionManager
import com.osscabinet.ossWallet.domain.repository.AssistantAccessRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton

/**
 * Turns the assistant functions on and off with the system. They ship disabled; only the user's switch in Settings
 * enables them. The switch is hidden on devices that can't run AppFunctions (before Android 16).
 */
@Singleton
class AssistantAccessController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val access: AssistantAccessRepository
) {
    /** True on Android 16+ when the system's AppFunctions support is present. */
    val isSupported: Boolean by lazy {
        Build.VERSION.SDK_INT >= MIN_SDK && runCatching { AppFunctionManager.getInstance(context) != null }.getOrDefault(false)
    }

    /** Saves the choice, then tells the system. Returns false if the system couldn't be updated (try again later). */
    suspend fun setEnabled(enabled: Boolean): Boolean {
        access.setEnabled(enabled)
        return applyToSystem(enabled)
    }

    /** Brings the system back in line with the saved choice (after a reinstall or restore); harmless to repeat. */
    suspend fun sync(): Boolean = applyToSystem(access.isEnabled())

    private suspend fun applyToSystem(enabled: Boolean): Boolean {
        if (!isSupported) return false
        val manager = AppFunctionManager.getInstance(context) ?: return false
        val state = if (enabled) AppFunctionManager.APP_FUNCTION_STATE_ENABLED else AppFunctionManager.APP_FUNCTION_STATE_DISABLED
        return try {
            FUNCTION_IDS.forEach { manager.setAppFunctionEnabled(it, state) }
            true
        } catch (e: Exception) {
            // Typically the system hasn't finished indexing the functions yet; the next sync will take.
            false
        }
    }

    companion object {
        private const val MIN_SDK = 36
        private const val SERVICE = "com.osscabinet.ossWallet.appfunctions.BaseOssWalletAppFunctionService"

        /** Written out rather than read from the class, which can't be loaded on devices before Android 16. */
        val FUNCTION_NAMES = listOf(
            "listAccounts", "listCategories", "listSavedFilters", "createTransaction", "searchTransactions", "getStatistics"
        )
        val FUNCTION_IDS: List<String> = FUNCTION_NAMES.map { "$SERVICE#$it" }
    }
}
