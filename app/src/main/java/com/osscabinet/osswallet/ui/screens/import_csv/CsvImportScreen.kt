package com.osscabinet.ossWallet.ui.screens.import_csv

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.ui.components.AeonHazeProvider
import com.osscabinet.ossWallet.ui.components.AeonTopAppBar
import com.osscabinet.ossWallet.ui.components.AeonTopBarNavTitlePill
import com.osscabinet.ossWallet.ui.components.AeonTopBarSection
import com.osscabinet.ossWallet.ui.components.aeonHazeSource
import com.osscabinet.ossWallet.ui.screens.import_csv.components.StepProgressIndicator
import com.osscabinet.ossWallet.ui.screens.import_csv.steps.ColumnMappingStep
import com.osscabinet.ossWallet.ui.screens.import_csv.steps.EntityMappingStep
import com.osscabinet.ossWallet.ui.screens.import_csv.steps.PreviewStep
import com.osscabinet.ossWallet.ui.screens.import_csv.steps.SourceSelectionStep
import com.osscabinet.ossWallet.ui.theme.AeonSize
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.HazeLayer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CsvImportScreen(
    viewModel: CsvImportViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDashboard: () -> Unit
) {
    val context = LocalContext.current
    val step by viewModel.step.collectAsStateWithLifecycle()
    val session by viewModel.session.collectAsStateWithLifecycle()
    val preview by viewModel.preview.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val loadingMessage by viewModel.loadingMessage.collectAsStateWithLifecycle()
    val result by viewModel.result.collectAsStateWithLifecycle()
    val importProgress by viewModel.importProgress.collectAsStateWithLifecycle()
    val errorMessage by viewModel.error.collectAsStateWithLifecycle()

    // Collecting ViewModel states for transient UI logic
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val shortlistedCurrencies by viewModel.shortlistedCurrencies.collectAsStateWithLifecycle()
    val accountMappings by viewModel.accountMappings.collectAsStateWithLifecycle()
    val categoryMappings by viewModel.categoryMappings.collectAsStateWithLifecycle()
    val parentGroupMappings by viewModel.parentGroupMappings.collectAsStateWithLifecycle()
    val selectedDuplicates by viewModel.selectedDuplicates.collectAsStateWithLifecycle()
    val duplicatePolicy by viewModel.duplicatePolicy.collectAsStateWithLifecycle()

    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.loadFile(it.toString(), context) }
    }

    if (result != null) {
        ImportResultScreen(
            result = result!!,
            onViewTransactions = onNavigateToDashboard,
            onReviewMappings = { viewModel.resetToColumnMapping() },
            onBack = onNavigateBack
        )
        return
    }

    if (errorMessage != null && session != null) {
        ImportErrorDialog(errorMessage!!, onDismiss = { viewModel.clearError() })
    }

    AeonHazeProvider(backgroundColor = AppColors.screenBackground) { hazeLayers ->
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(containerColor = Color.Transparent) { innerPadding ->
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .aeonHazeSource(hazeLayers.states[HazeLayer.Overlay])
                            .statusBarsPadding()
                            .padding(
                                top = AeonSize.D64.value + AeonTheme.dimensions.screenPadding,
                                bottom = innerPadding.calculateBottomPadding()
                            )
                            .padding(horizontal = AeonTheme.dimensions.screenPadding)
                    ) {
                        StepProgressIndicator(getStepIndex(step))
                        Spacer(Modifier.height(AeonTheme.dimensions.spacingMd))

                        Box(modifier = Modifier.fillMaxSize()) {
                            StepContent(
                                step = step,
                                session = session,
                                preview = preview,
                                isLoading = isLoading,
                                errorMessage = errorMessage,
                                accounts = accounts,
                                categories = categories,
                                shortlistedCurrencies = shortlistedCurrencies,
                                accountMappings = accountMappings,
                                categoryMappings = categoryMappings,
                                parentGroupMappings = parentGroupMappings,
                                selectedDuplicates = selectedDuplicates,
                                duplicatePolicy = duplicatePolicy,
                                viewModel = viewModel,
                                fileLauncher = fileLauncher,
                                context = context
                            )

                            if (isLoading) {
                                LoadingBanner(loadingMessage, importProgress)
                            }
                        }
                    }
                }
            }

            AeonTopAppBar(
                left = AeonTopBarSection(
                    content = {
                        AeonTopBarNavTitlePill(
                            title = "Import Transactions",
                            onBackClick = onNavigateBack
                        )
                    }
                )
            )
        }
    }
}

@Composable
private fun StepContent(
    step: CsvImportViewModel.Step,
    session: com.osscabinet.ossWallet.domain.model.import_csv.CsvImportSession?,
    preview: com.osscabinet.ossWallet.domain.model.import_csv.ImportPreview?,
    isLoading: Boolean,
    errorMessage: String?,
    accounts: List<com.osscabinet.ossWallet.domain.model.Account>,
    categories: List<Category>,
    shortlistedCurrencies: List<String>,
    accountMappings: Map<String, com.osscabinet.ossWallet.domain.model.import_csv.EntityMapping>,
    categoryMappings: Map<String, com.osscabinet.ossWallet.domain.model.import_csv.EntityMapping>,
    parentGroupMappings: Map<String, String?>,
    selectedDuplicates: Set<Int>,
    duplicatePolicy: com.osscabinet.ossWallet.domain.model.import_csv.DuplicatePolicy,
    viewModel: CsvImportViewModel,
    fileLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    context: android.content.Context
) {
    if (isLoading && session == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = AppColors.primary)
        }
    } else {
        Box(modifier = Modifier.alpha(if (isLoading) 0.4f else 1f)) {
            when (step) {
                is CsvImportViewModel.Step.SourceSelection -> SourceSelectionStep(
                    session = session,
                    onLoadFile = { fileLauncher.launch("*/*") },
                    onConfirm = { app -> viewModel.confirmSourceApp(app) },
                    fileLoadError = if (session == null) errorMessage else null
                )

                is CsvImportViewModel.Step.ColumnMapping -> ColumnMappingStep(
                    session = session!!,
                    onUpdateMapping = { field, col -> viewModel.updateMapping(field, col) },
                    onApplyPreset = { viewModel.applyPreset(it) },
                    onNext = { viewModel.proceedFromColumnMapping(context) },
                    isLoading = isLoading
                )

                is CsvImportViewModel.Step.EntityMapping -> EntityMappingStep(
                    preview = preview,
                    accounts = accounts.map { it.id to it.name },
                    categories = categories.map { it.id.toString() to it.name },
                    rootCategories = categories
                        .filter { it.parentId == null && !Category.isSystemCategory(it.id) }
                        .map { it.id.toString() to it.name },
                    shortlistedCurrencies = shortlistedCurrencies,
                    accountMappings = accountMappings,
                    categoryMappings = categoryMappings,
                    parentGroupMappings = parentGroupMappings,
                    onUpdateAccountMapping = { name, mapping ->
                        viewModel.updateAccountMapping(
                            name,
                            mapping
                        )
                    },
                    onUpdateCategoryMapping = { name, mapping ->
                        viewModel.updateCategoryMapping(
                            name,
                            mapping
                        )
                    },
                    onUpdateParentGroupMapping = { name, id ->
                        viewModel.updateParentGroupMapping(
                            name,
                            id
                        )
                    },
                    onConfirmMappings = { viewModel.proceedFromEntityMapping() }
                )

                is CsvImportViewModel.Step.Preview -> PreviewStep(
                    preview = preview,
                    duplicatePolicy = duplicatePolicy,
                    selectedDuplicates = selectedDuplicates,
                    onSetDuplicatePolicy = { viewModel.setDuplicatePolicy(it) },
                    onToggleDuplicate = { viewModel.toggleDuplicateRow(it) },
                    onImport = { viewModel.executeImport(context) },
                    isLoading = isLoading
                )
            }
        }
    }
}

@Composable
private fun LoadingBanner(message: String?, progress: Float) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = AeonTheme.dimensions.spacingLg),
        color = AppColors.cardSurface,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(AeonTheme.dimensions.cardPadding)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(
                    AeonTheme.dimensions.spacingMd
                )
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = AppColors.primary
                )
                Text(
                    text = message ?: "Working…",
                    style = AppTextStyles.listItemTitle,
                    color = AppColors.contentPrimary
                )
            }
            if (progress > 0f) {
                Spacer(Modifier.height(AeonTheme.dimensions.spacingSm))
                androidx.compose.material3.LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = AppColors.primary,
                    trackColor = AppColors.outlineVariant,
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }
        }
    }
}

@Composable
private fun ImportErrorDialog(error: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Something went wrong") },
        text = { Text(error) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK", style = AppTextStyles.buttonLabel, color = AppColors.primary)
            }
        }
    )
}

private fun getStepIndex(step: CsvImportViewModel.Step): Int = when (step) {
    is CsvImportViewModel.Step.SourceSelection -> 0
    is CsvImportViewModel.Step.ColumnMapping -> 1
    is CsvImportViewModel.Step.EntityMapping -> 2
    is CsvImportViewModel.Step.Preview -> 3
}
