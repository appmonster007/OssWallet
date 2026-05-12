package com.osscabinet.ossWallet.ui.screens.accounts.entry

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.ui.viewmodel.AccountViewModel
import com.osscabinet.ossWallet.ui.viewmodel.TransactionViewModel
import com.osscabinet.ossWallet.ui.screens.accounts.entry.components.AccountFormBody

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountEntryScreen(
    onBackClick: () -> Unit,
    onDone: () -> Unit,
    viewModel: AccountViewModel,
    transactionViewModel: TransactionViewModel,
    accountId: String? = null
) {
    val isEditMode = accountId != null
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val account = if (isEditMode) accounts.find { it.id == accountId } else null

    // Show a loader until the account is available in edit mode
    if (isEditMode && account == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    var navTriggered by remember { mutableStateOf(false) }
    val navigate: (() -> Unit) -> Unit = { action ->
        if (!navTriggered) {
            navTriggered = true
            action()
        }
    }

    // State for saving (managed in the screen for now, matching original logic)
    var canSave by remember { mutableStateOf(false) }
    var onSaveTrigger by remember { mutableStateOf<(() -> Unit)?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        if (isEditMode) "Edit Account" else "New Account",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navigate(onBackClick) }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isEditMode) {
                        IconButton(
                            onClick = {
                                navigate {
                                    viewModel.deleteAccount(accountId!!)
                                    onBackClick()
                                }
                            }
                        ) {
                            Icon(
                                Icons.Rounded.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    IconButton(
                        onClick = { onSaveTrigger?.invoke() },
                        enabled = canSave
                    ) {
                        Icon(
                            Icons.Rounded.Done,
                            contentDescription = "Save",
                            tint = if (canSave) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        AccountFormBody(
            account = account,
            viewModel = viewModel,
            transactionViewModel = transactionViewModel,
            isEditMode = isEditMode,
            accountId = accountId,
            onValidationChanged = { valid -> canSave = valid },
            registerSaveAction = { saveAction -> onSaveTrigger = { navigate(saveAction) } },
            onBackClick = onBackClick,
            onDone = onDone,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
