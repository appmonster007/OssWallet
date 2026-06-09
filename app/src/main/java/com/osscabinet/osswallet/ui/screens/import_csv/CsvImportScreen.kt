package com.osscabinet.ossWallet.ui.screens.import_csv

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BrokenImage
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.import_csv.AccountToMap
import com.osscabinet.ossWallet.domain.model.import_csv.CsvImportSession
import com.osscabinet.ossWallet.domain.model.import_csv.ImportSummary
import com.osscabinet.ossWallet.domain.model.import_csv.DuplicatePolicy
import com.osscabinet.ossWallet.domain.model.import_csv.EntityMapping
import com.osscabinet.ossWallet.domain.model.import_csv.ImportPreview
import com.osscabinet.ossWallet.domain.model.import_csv.ImportSourceApp
import com.osscabinet.ossWallet.domain.model.import_csv.OssWalletField
import com.osscabinet.ossWallet.ui.components.AeonDashedButton
import com.osscabinet.ossWallet.ui.components.AeonHazeProvider
import com.osscabinet.ossWallet.ui.components.AeonHeroHeader
import com.osscabinet.ossWallet.ui.components.AeonSectionHeader
import com.osscabinet.ossWallet.ui.components.AeonTopAppBar
import com.osscabinet.ossWallet.ui.components.AeonTopBarNavTitlePill
import com.osscabinet.ossWallet.ui.components.AeonTopBarSection
import com.osscabinet.ossWallet.ui.components.aeonHazeSource
import com.osscabinet.ossWallet.ui.components.cards.AeonExpressiveCard
import com.osscabinet.ossWallet.ui.theme.AeonSize
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.HazeLayer
import com.osscabinet.ossWallet.ui.theme.WarningColor

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
    val error by viewModel.error.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()

    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.loadFile(it.toString(), context) }
    }

    val stepIndex = when (step) {
        is CsvImportViewModel.Step.SourceSelection -> 0
        is CsvImportViewModel.Step.ColumnMapping   -> 1
        is CsvImportViewModel.Step.EntityMapping   -> 2
        is CsvImportViewModel.Step.Preview         -> 3
    }

    // Full-screen result state replaces the wizard
    if (result != null) {
        ImportResultScreen(
            result = result!!,
            onViewTransactions = onNavigateToDashboard,
            onReviewMappings = { viewModel.resetToColumnMapping() },
            onBack = onNavigateBack
        )
        return
    }

    // Inline error for file-load failures (when no session yet)
    val showErrorDialog = error != null && session != null
    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Something went wrong") },
            text = { Text(error ?: "") },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) { 
                    Text("OK", style = AppTextStyles.buttonLabel, color = AppColors.primary) 
                }
            }
        )
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
                        StepProgressIndicator(stepIndex)
                        Spacer(Modifier.height(AeonTheme.dimensions.spacingMd))

                        // Content with loading overlay
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (isLoading && session == null) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = AppColors.primary)
                                }
                            } else {
                                // Step content — dimmed while loading
                                Box(modifier = Modifier.alpha(if (isLoading) 0.4f else 1f)) {
                                    when (step) {
                                    is CsvImportViewModel.Step.SourceSelection -> SourceSelectionStep(
                                        session = session,
                                        onLoadFile = { fileLauncher.launch("*/*") },
                                        onConfirm = { app -> viewModel.confirmSourceApp(app) },
                                        fileLoadError = if (session == null) error else null
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
                                        onConfirmMappings = { mappings ->
                                            viewModel.setEntityMappings(mappings)
                                            viewModel.proceedFromEntityMapping()
                                        }
                                    )
                                    is CsvImportViewModel.Step.Preview -> PreviewStep(
                                        preview = preview,
                                        onSetDuplicatePolicy = { viewModel.setDuplicatePolicy(it) },
                                        onToggleDuplicate = { viewModel.toggleDuplicateRow(it) },
                                        onImport = { viewModel.executeImport(context) },
                                        isLoading = isLoading
                                    )
                                }
                            }
                        }

                            // Loading banner — bottom-docked, contextual message
                            if (isLoading) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = AeonTheme.dimensions.spacingLg),
                                    color = AppColors.cardSurface,
                                    shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
                                    shadowElevation = 4.dp
                                ) {
                                    Row(
                                        modifier = Modifier.padding(AeonTheme.dimensions.cardPadding),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp,
                                            color = AppColors.primary
                                        )
                                        Text(
                                            text = loadingMessage ?: "Working…",
                                            style = AppTextStyles.listItemTitle,
                                            color = AppColors.contentPrimary
                                        )
                                    }
                                }
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

// ─── Step 1: Source App Selection ────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SourceSelectionStep(
    session: CsvImportSession?,
    onLoadFile: () -> Unit,
    onConfirm: (ImportSourceApp) -> Unit,
    fileLoadError: String?
) {
    var selected by remember(session?.sourceApp) {
        mutableStateOf(session?.sourceApp ?: ImportSourceApp.GENERIC)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
    ) {
        if (session == null) {
            if (fileLoadError != null) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingXl)
                ) {
                    AeonHeroHeader(
                        title = "File Error",
                        subtitle = "Could not open CSV file",
                        iconName = "BrokenImage",
                        accentColorHex = "#E53935",
                        isLeftAligned = false
                    )
                    Text(
                        "The file may have been moved or is not a valid CSV. Try choosing a different file.",
                        style = AppTextStyles.caption,
                        color = AppColors.contentSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = AeonTheme.dimensions.spacingLg)
                    )
                    AeonDashedButton(
                        onClick = onLoadFile,
                        text = "Try Another File",
                        icon = Icons.Rounded.AttachFile
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingXl)
                ) {
                    AeonHeroHeader(
                        title = "Import Transactions",
                        subtitle = "Supports BudgetBakers, Cashew and Generic CSV",
                        iconName = "FileOpen",
                        accentColorHex = "#004D5F",
                        isLeftAligned = false
                    )
                    Text(
                        "Supported formats include Wallet by BudgetBakers, Cashew, Money Manager, and any CSV with a valid header row.",
                        style = AppTextStyles.caption,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = AppColors.contentSecondary,
                        modifier = Modifier.padding(horizontal = AeonTheme.dimensions.spacingLg)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm),
                    ) {
                        listOf("BudgetBakers", "Cashew", "Any CSV").forEach { label ->
                            Surface(
                                color = AppColors.cardSurfaceRaised,
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    label,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = AppTextStyles.chipLabel,
                                    color = AppColors.contentSecondary
                                )
                            }
                        }
                    }
                    AeonDashedButton(
                        onClick = onLoadFile,
                        text = "Choose CSV File",
                        icon = Icons.Rounded.AttachFile
                    )
                }
            }
        } else {
            Text(
                "Where is this file from?",
                style = AppTextStyles.heroAmount,
                color = AppColors.contentPrimary
            )
            Text(
                "Select the app you exported from. OssWallet uses this to apply the right import rules.",
                style = AppTextStyles.caption,
                color = AppColors.contentSecondary
            )

            if (session.sourceApp != null && session.sourceApp != ImportSourceApp.GENERIC) {
                Surface(
                    color = AppColors.primaryContainer,
                    shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius)
                ) {
                    Row(
                        modifier = Modifier.padding(AeonTheme.dimensions.cardPadding),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
                    ) {
                        Icon(
                            Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = AppColors.primary
                        )
                        Text(
                            "Auto-detected as ${session.sourceApp.displayName}",
                            style = AppTextStyles.caption,
                            color = AppColors.onPrimaryContainer
                        )
                    }
                }
            }

            ImportSourceApp.entries.forEach { app ->
                val isSelected = selected == app
                Surface(
                    onClick = { selected = app },
                    shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
                    color = if (isSelected) AppColors.primaryContainer else AppColors.cardSurface,
                    border = BorderStroke(
                        if (isSelected) 1.5.dp else 0.5.dp,
                        if (isSelected) AppColors.primary else AppColors.outlineVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AeonTheme.dimensions.cardPadding),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
                    ) {
                        RadioButton(selected = isSelected, onClick = { selected = app })
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                app.displayName,
                                style = AppTextStyles.listItemTitle,
                                color = AppColors.contentPrimary
                            )
                            Text(
                                when (app) {
                                    ImportSourceApp.BUDGET_BAKERS -> "Wallet by BudgetBakers — semicolon-delimited"
                                    ImportSourceApp.CASHEW        -> "Cashew — comma-delimited with subcategories"
                                    ImportSourceApp.MONEY_MANAGER -> "Money Manager — standard CSV"
                                    ImportSourceApp.GENERIC       -> "Any other CSV with a header row"
                                },
                                style = AppTextStyles.caption,
                                color = AppColors.contentSecondary
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(AeonTheme.dimensions.spacingLg))
            AeonDashedButton(
                onClick = { onConfirm(selected) },
                text = "Continue",
                icon = Icons.AutoMirrored.Rounded.ArrowForward
            )
            Spacer(Modifier.height(AeonTheme.dimensions.spacingLg))
        }
    }
}

// ─── Step 2: Column Mapping ───────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColumnMappingStep(
    session: CsvImportSession,
    onUpdateMapping: (OssWalletField, Int?) -> Unit,
    onApplyPreset: (ImportSourceApp) -> Unit,
    onNext: () -> Unit,
    isLoading: Boolean
) {
    var customiseMappings by remember { mutableStateOf(false) }

    // Reset customization view when preset changes
    LaunchedEffect(session.sourceApp) {
        customiseMappings = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
    ) {
        val isBudgetBakers = session.sourceApp == ImportSourceApp.BUDGET_BAKERS
        val isCashew = session.sourceApp == ImportSourceApp.CASHEW
        val hasPreset = isBudgetBakers || isCashew

            // Source app dropdown
            var presetExpanded by remember { mutableStateOf(false) }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm)
            ) {
                Text(
                    "Import as:",
                    style = AppTextStyles.caption,
                    color = AppColors.contentSecondary
                )
                Box {
                    Surface(
                        onClick = { presetExpanded = true },
                        shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
                        color = AppColors.surfaceContainerLow,
                        border = BorderStroke(1.dp, AppColors.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                session.sourceApp?.displayName ?: "Select app…",
                                style = AppTextStyles.caption,
                                color = if (session.sourceApp != null) AppColors.contentPrimary else AppColors.contentSecondary
                            )
                            Icon(
                                imageVector = if (presetExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                contentDescription = null,
                                tint = AppColors.contentSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = presetExpanded,
                        onDismissRequest = { presetExpanded = false },
                        containerColor = AppColors.surfaceContainerHigh,
                        shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius)
                    ) {
                        ImportSourceApp.entries.forEach { app ->
                            DropdownMenuItem(
                                text = { Text(app.displayName, style = AppTextStyles.caption) },
                                onClick = {
                                    onApplyPreset(app)
                                    presetExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            AeonSectionHeader(
                title = "Preview \u2014 first ${session.previewRows.size} rows",
            )
            Surface(
                color = AppColors.cardSurface,
                shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
                border = BorderStroke(0.5.dp, AppColors.outlineVariant)
            ) {
                Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    Column {
                        // Header row
                        Row(modifier = Modifier.background(AppColors.cardSurfaceRaised)) {
                            session.headers.forEach { header ->
                                Text(
                                    text = header,
                                    modifier = Modifier.width(120.dp).padding(horizontal = 12.dp, vertical = 8.dp),
                                    style = AppTextStyles.tableColumnHeader,
                                    color = AppColors.contentSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    softWrap = false
                                )
                            }
                        }
                        // Data rows with alternating background
                        session.previewRows.take(3).forEachIndexed { rowIdx, row ->
                            val rowBg = if (rowIdx % 2 == 0) AppColors.cardSurface else AppColors.cardSurfaceRaised
                            Row(modifier = Modifier.background(rowBg)) {
                                row.forEach { cell ->
                                    Text(
                                        text = cell,
                                        modifier = Modifier.width(120.dp).padding(horizontal = 12.dp, vertical = 8.dp),
                                        style = AppTextStyles.caption,
                                        color = AppColors.contentSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = AppColors.divider.copy(alpha = 0.5f))

            // Column mapping section
            if (hasPreset && !customiseMappings) {
                // Read-only mode for auto-detected BudgetBakers
                Text(
                    "Column Mappings",
                    style = AppTextStyles.formSectionLabel,
                    color = AppColors.contentSecondary
                )
                OssWalletField.entries.forEach { field ->
                    val colIndex = session.mapping[field]
                    val colName = if (colIndex != null && colIndex < session.headers.size) {
                        session.headers[colIndex]
                    } else null
                    if (colName != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                field.displayName,
                                style = AppTextStyles.listItemTitle,
                                color = AppColors.contentPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                colName,
                                style = AppTextStyles.caption,
                                color = AppColors.contentSecondary
                            )
                        }
                    }
                }
                TextButton(onClick = { customiseMappings = true }) {
                    Text("Customise mappings", style = AppTextStyles.buttonLabel, color = AppColors.primary)
                }
            } else {
                // Editable dropdown mapping
                Text(
                    "Map Columns",
                    style = AppTextStyles.formSectionLabel,
                    color = AppColors.contentSecondary
                )

                OssWalletField.entries.forEach { field ->
                    var expanded by remember { mutableStateOf(false) }
                    val currentColIndex = session.mapping[field]
                    val selectedText = if (currentColIndex != null && currentColIndex < session.headers.size) {
                        session.headers[currentColIndex]  // drop the index prefix
                    } else "Skip"

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = field.displayName,
                                modifier = Modifier.weight(1f),
                                style = AppTextStyles.listItemTitle,
                                color = AppColors.contentPrimary
                            )
                            Spacer(Modifier.width(8.dp))
                            // Trigger selection box
                            Box(modifier = Modifier.weight(1.5f)) {
                                Surface(
                                    onClick = { expanded = true },
                                    shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
                                    color = AppColors.surfaceContainerLow,
                                    border = BorderStroke(1.dp, AppColors.outlineVariant),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = selectedText,
                                            style = AppTextStyles.caption,
                                            color = if (currentColIndex != null) AppColors.contentPrimary else AppColors.contentSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = AppColors.contentSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false },
                                    containerColor = AppColors.surfaceContainerHigh,
                                    shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
                                    modifier = Modifier.widthIn(min = 160.dp)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Skip", style = AppTextStyles.caption) },
                                        onClick = {
                                            onUpdateMapping(field, null)
                                            expanded = false
                                        }
                                    )
                                    session.headers.forEachIndexed { index, header ->
                                        DropdownMenuItem(
                                            text = { Text(header, style = AppTextStyles.caption) },
                                            onClick = {
                                                onUpdateMapping(field, index)
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        // Helper text for ambiguous fields
                        val helperText = when (field) {
                            OssWalletField.IS_TRANSFER -> "Identifies paired transfer rows (e.g. credit card payments)"
                            OssWalletField.WARRANTY_MONTHS -> "Number of months warranty remaining on a purchased item"
                            OssWalletField.LABELS -> "Comma or pipe-separated tags attached to the transaction"
                            else -> null
                        }
                        if (helperText != null) {
                            Text(
                                helperText,
                                style = AppTextStyles.caption,
                                color = AppColors.contentSecondary,
                                modifier = Modifier.padding(start = 2.dp, top = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(AeonTheme.dimensions.spacingLg))
            AeonDashedButton(
                onClick = onNext,
                text = "Continue",
                icon = Icons.AutoMirrored.Rounded.ArrowForward,
                enabled = !isLoading
            )
            Spacer(Modifier.height(AeonTheme.dimensions.spacingLg))
    }
}

// ─── Step 2: Entity Mapping ───────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntityMappingStep(
    preview: ImportPreview?,
    accounts: List<Pair<String, String>>,
    categories: List<Pair<String, String>>,
    rootCategories: List<Pair<String, String>>,
    onConfirmMappings: (List<EntityMapping>) -> Unit
) {
    if (preview == null) return

    data class LocalCategoryGroup(
        val key: String, // parentName or "" for top-level
        val parentName: String?, // null = top-level items
        val items: List<Pair<Int, com.osscabinet.ossWallet.domain.model.import_csv.CategoryToMap>>
    )

    val categoryGroups = remember(preview.newCategories) {
        preview.newCategories
            .mapIndexed { i, cat -> i to cat }
            .groupBy { (_, cat) -> cat.suggestedParentName ?: "" }
            .entries
            .map { (key, items) ->
                LocalCategoryGroup(
                    key = key,
                    parentName = key.takeIf { it.isNotBlank() },
                    items = items
                )
            }
            .sortedWith(compareBy(nullsLast()) { it.parentName })
    }

    // "" = root-level items sentinel, non-empty = parentName
    var activeCategoryGroupKey by remember { mutableStateOf<String?>(null) }

    // parent group mappings: key=parentName \u2192 existingId (null = create new)
    val parentGroupMappings = remember(preview.newCategories) {
        androidx.compose.runtime.mutableStateMapOf<String, String?>()
    }

    val accountMappings = remember(preview.allCsvAccounts) {
        mutableStateListOf(*preview.allCsvAccounts.map { acc ->
            EntityMapping(
                unknownName = acc.name,
                existingId = acc.suggestedExistingId,
                entityType = EntityMapping.EntityType.ACCOUNT
            )
        }.toTypedArray())
    }
    val categoryMappings = remember(preview.newCategories) {
        mutableStateListOf(*preview.newCategories.map { cat ->
            EntityMapping(cat.name, null, EntityMapping.EntityType.CATEGORY)
        }.toTypedArray())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
    ) {
        if (activeCategoryGroupKey == null) {
            // \u2500\u2500 Top-level view: accounts + category group list \u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500
            Text(
                "Accounts & Categories",
                style = AppTextStyles.heroAmount,
                color = AppColors.contentPrimary
            )
            Text(
                "Map accounts and categories. Tap a category group to configure its mappings.",
                style = AppTextStyles.caption,
                color = AppColors.contentSecondary
            )

            if (preview.allCsvAccounts.isNotEmpty()) {
                Spacer(Modifier.height(AeonTheme.dimensions.spacingSm))
                AeonSectionHeader(
                    title = "ACCOUNTS \u2014 ${preview.allCsvAccounts.size} found",
                    trailing = {
                        TextButton(onClick = {
                            accountMappings.indices.forEach { i ->
                                accountMappings[i] = accountMappings[i].copy(existingId = null)
                            }
                        }) {
                            Text("Create all new", style = AppTextStyles.buttonLabel, color = AppColors.primary)
                        }
                    }
                )
                preview.allCsvAccounts.forEachIndexed { index, acc ->
                    val em = accountMappings.getOrNull(index)
                    val isExcluded = em?.existingId == "__EXCLUDE__"
                    EntityMappingRow(
                        label = acc.name,
                        badgeText = if (isExcluded) "${acc.rowCount} rows skipped" else null,
                        chipText = if (!isExcluded) acc.suggestedExistingName?.let { "matched: $it" } else null,
                        options = accounts,
                        selectedId = em?.existingId,
                        onSelect = { id -> accountMappings[index] = accountMappings[index].copy(existingId = id) },
                        showExcludeOption = true
                    )
                }
            }

            if (categoryGroups.isNotEmpty()) {
                Spacer(Modifier.height(AeonTheme.dimensions.spacingSm))
                AeonSectionHeader(
                    title = "CATEGORIES \u2014 ${preview.newCategories.size} new"
                )

                categoryGroups.forEach { group ->
                    val mappedCount = group.items.count { (idx, _) ->
                        categoryMappings.getOrNull(idx)?.existingId != null
                    }
                    val allMapped = mappedCount == group.items.size

                    Surface(
                        onClick = { activeCategoryGroupKey = group.key.ifEmpty { "__ROOT__" } },
                        color = AppColors.cardSurface,
                        shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
                        border = BorderStroke(0.5.dp, AppColors.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(AeonTheme.dimensions.cardPadding),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = group.parentName ?: "Top-level categories",
                                    style = AppTextStyles.listItemTitle,
                                    color = AppColors.contentPrimary
                                )
                                Text(
                                    text = if (group.items.size == 1) "1 category"
                                           else "${group.items.size} categories",
                                    style = AppTextStyles.caption,
                                    color = AppColors.contentSecondary
                                )
                            }
                            Surface(
                                color = if (allMapped) AppColors.primaryContainer else AppColors.accentContainer.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = if (allMapped) "Mapped" else "$mappedCount/${group.items.size}",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    style = AppTextStyles.chipLabel,
                                    color = if (allMapped) AppColors.primary else AppColors.contentSecondary
                                )
                            }
                            Icon(
                                Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = AppColors.contentSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(AeonTheme.dimensions.spacingLg))
            AeonDashedButton(
                onClick = {
                    val resolvedAccountMappings = accountMappings.map { em ->
                        if (em.existingId == "__EXCLUDE__") em.copy(existingId = null, isExcluded = true) else em
                    }
                    val resolvedCategoryMappings = categoryMappings.mapIndexed { i, em ->
                        val cat = preview.newCategories.getOrNull(i)
                        val parentName = cat?.suggestedParentName
                        if (em.existingId != null || parentName == null) {
                            em
                        } else {
                            val parentExistingId = parentGroupMappings[parentName]
                            if (parentExistingId != null) {
                                em.copy(parentCategoryId = parentExistingId.toLongOrNull())
                            } else {
                                em.copy(parentCategoryName = parentName)
                            }
                        }
                    }
                    onConfirmMappings(resolvedAccountMappings + resolvedCategoryMappings)
                },
                text = "Continue to Preview",
                icon = Icons.AutoMirrored.Rounded.ArrowForward
            )
            Spacer(Modifier.height(AeonTheme.dimensions.spacingLg))

        } else {
            // \u2500\u2500 Group detail view \u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500
            val activeKey = activeCategoryGroupKey
            val group = categoryGroups.find { g ->
                (g.key.isEmpty() && activeKey == "__ROOT__") || g.key == activeKey
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    onClick = { activeCategoryGroupKey = null },
                    color = Color.Transparent,
                    shape = RoundedCornerShape(50)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = AppColors.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "Back",
                            style = AppTextStyles.buttonLabel,
                            color = AppColors.primary
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                if (group != null) {
                    Text(
                        group.parentName ?: "Top-level",
                        style = AppTextStyles.overlineLabel,
                        color = AppColors.contentSecondary
                    )
                }
            }

            if (group != null) {
                // If this group has a named parent and no existing root match, let user map the parent
                val parentName = group.parentName
                if (parentName != null) {
                    val existingRootMatch = rootCategories.find { it.second.equals(parentName, ignoreCase = true) }

                    AeonSectionHeader(title = "PARENT CATEGORY")
                    if (existingRootMatch != null && parentGroupMappings[parentName] == null) {
                        // Pre-fill suggested match on first open
                        parentGroupMappings[parentName] = existingRootMatch.first
                    }
                    EntityMappingRow(
                        label = "\"$parentName\"",
                        badgeText = null,
                        chipText = existingRootMatch?.let { "matched: ${it.second}" },
                        options = rootCategories,
                        selectedId = parentGroupMappings[parentName],
                        onSelect = { id -> parentGroupMappings[parentName] = id }
                    )

                    Spacer(Modifier.height(AeonTheme.dimensions.spacingSm))
                }

                AeonSectionHeader(
                    title = if (parentName != null) "SUB-CATEGORIES" else "CATEGORIES"
                )
                group.items.forEach { (globalIdx, cat) ->
                    EntityMappingRow(
                        label = cat.name,
                        badgeText = null,
                        chipText = cat.envelopeId?.let { "#$it" },
                        options = categories,
                        selectedId = categoryMappings.getOrNull(globalIdx)?.existingId,
                        onSelect = { id ->
                            if (globalIdx < categoryMappings.size) {
                                categoryMappings[globalIdx] = categoryMappings[globalIdx].copy(existingId = id)
                            }
                        }
                    )
                }

                Spacer(Modifier.height(AeonTheme.dimensions.spacingLg))
                AeonDashedButton(
                    onClick = { activeCategoryGroupKey = null },
                    text = "Done with this group",
                    icon = Icons.Rounded.CheckCircle
                )
                Spacer(Modifier.height(AeonTheme.dimensions.spacingLg))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntityMappingRow(
    label: String,
    badgeText: String?,
    chipText: String?,
    options: List<Pair<String, String>>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
    isError: Boolean = false,
    showNullOption: Boolean = true,
    showExcludeOption: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    val isExcluded = selectedId == "__EXCLUDE__"
    val selectedName = when {
        isExcluded -> "Excluded"
        selectedId != null -> options.find { it.first == selectedId }?.second ?: "Create New"
        else -> "Create New"
    }

    Surface(
        color = AppColors.cardSurface,
        shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AeonTheme.dimensions.cardPadding, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    style = AppTextStyles.listItemTitle,
                    color = if (isError) AppColors.error else AppColors.contentPrimary
                )
                if (badgeText != null || chipText != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (chipText != null) {
                            Surface(
                                color = AppColors.accentContainer,
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    chipText,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = AppTextStyles.chipLabel,
                                    color = AppColors.onAccentContainer
                                )
                            }
                        }
                        if (badgeText != null) {
                            Text(
                                badgeText,
                                style = AppTextStyles.chipLabel,
                                color = AppColors.contentSecondary
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            Box(modifier = Modifier.weight(1.4f)) {
                Surface(
                    onClick = { expanded = true },
                    shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
                    color = when {
                        isExcluded -> AppColors.error.copy(alpha = 0.08f)
                        isError -> AppColors.errorContainer.copy(alpha = 0.1f)
                        else -> AppColors.surfaceContainerLow
                    },
                    border = BorderStroke(1.dp, when {
                        isExcluded -> AppColors.error.copy(alpha = 0.5f)
                        isError -> AppColors.error
                        else -> AppColors.outlineVariant
                    }),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = selectedName,
                            style = AppTextStyles.caption,
                            color = when {
                                isExcluded -> AppColors.error
                                selectedId != null -> AppColors.contentPrimary
                                else -> AppColors.contentSecondary
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                            contentDescription = null,
                            tint = AppColors.contentSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    containerColor = AppColors.surfaceContainerHigh,
                    shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
                    modifier = Modifier.widthIn(min = 160.dp)
                ) {
                    if (showNullOption) {
                        DropdownMenuItem(
                            text = { Text("Create New", style = AppTextStyles.caption) },
                            onClick = { onSelect(null); expanded = false }
                        )
                    }
                    if (showExcludeOption) {
                        DropdownMenuItem(
                            text = { Text("Exclude account", style = AppTextStyles.caption, color = AppColors.error) },
                            onClick = { onSelect("__EXCLUDE__"); expanded = false }
                        )
                    }
                    options.forEach { (id, name) ->
                        DropdownMenuItem(
                            text = { Text(name, style = AppTextStyles.caption) },
                            onClick = { onSelect(id); expanded = false }
                        )
                    }
                }
            }
        }
    }
}

// ─── Step 3: Preview & Confirm ────────────────────────────────────────────────

@Composable
private fun PreviewStep(
    preview: ImportPreview?,
    onSetDuplicatePolicy: (DuplicatePolicy) -> Unit,
    onToggleDuplicate: (Int) -> Unit,
    onImport: () -> Unit,
    isLoading: Boolean
) {
    if (preview == null) return

    var duplicatePolicy by remember { mutableStateOf(DuplicatePolicy.SKIP) }
    var showDuplicates by remember { mutableStateOf(false) }
    var showErrors by remember { mutableStateOf(false) }
    val selectedDuplicateRows = remember { mutableStateListOf<Int>() }

    // Effective import count based on selected policy
    val effectiveCount = when (duplicatePolicy) {
        DuplicatePolicy.SKIP -> preview.validRows - preview.duplicates.size
        else -> preview.validRows
    }.coerceAtLeast(0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
    ) {
        // Summary card
        AeonExpressiveCard(
            title = "Import Summary",
            icon = Icons.Rounded.Info,
            containerColor = AppColors.cardSurface,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    // Top-left: hero — effective import count
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            effectiveCount.toString(),
                            style = AppTextStyles.heroAmount.copy(fontWeight = FontWeight.Bold),
                            color = AppColors.primary
                        )
                        Text(
                            "will be imported",
                            style = AppTextStyles.caption,
                            color = AppColors.contentSecondary
                        )
                    }
                    // Top-right: total rows
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            preview.totalRows.toString(),
                            style = AppTextStyles.heroAmount,
                            color = AppColors.contentSecondary
                        )
                        Text(
                            "total rows",
                            style = AppTextStyles.caption,
                            color = AppColors.contentSecondary
                        )
                    }
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    // Bottom-left: duplicates
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            preview.duplicates.size.toString(),
                            style = AppTextStyles.heroAmount,
                            color = if (preview.duplicates.isNotEmpty()) AppColors.accent else AppColors.contentSecondary
                        )
                        Text(
                            "duplicates",
                            style = AppTextStyles.caption,
                            color = AppColors.contentSecondary
                        )
                    }
                    // Bottom-right: parse errors
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            preview.parseErrors.size.toString(),
                            style = AppTextStyles.heroAmount,
                            color = if (preview.parseErrors.isNotEmpty()) AppColors.error else AppColors.contentSecondary
                        )
                        Text(
                            "parse errors",
                            style = AppTextStyles.caption,
                            color = AppColors.contentSecondary
                        )
                    }
                }
            }
        }

        // Unpaired transfer warning
        if (preview.unpairedTransferRows > 0) {
            Surface(
                color = AppColors.errorContainer,
                shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius)
            ) {
                Row(
                    modifier = Modifier.padding(AeonTheme.dimensions.cardPadding),
                    horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = AppColors.error,
                        modifier = Modifier.size(20.dp).padding(top = 2.dp)
                    )
                    Column {
                        Text(
                            "${preview.unpairedTransferRows} unpaired transfer row${if (preview.unpairedTransferRows > 1) "s" else ""}",
                            style = AppTextStyles.cardSectionTitle,
                            color = AppColors.onErrorContainer
                        )
                        Text(
                            "Some transfer transactions are missing their counterpart in this file. These will be imported as outside-wallet transfers (money sent to or received from outside OssWallet).",
                            style = AppTextStyles.caption,
                            color = AppColors.onErrorContainer
                        )
                    }
                }
            }
        }

        // Duplicate policy
        if (preview.duplicates.isNotEmpty()) {
            HorizontalDivider(color = AppColors.divider.copy(alpha = 0.5f))
            Text(
                "DUPLICATE POLICY",
                style = AppTextStyles.formSectionLabel,
                color = AppColors.contentSecondary
            )
            DuplicatePolicy.entries.forEach { policy ->
                val (title, icon) = when (policy) {
                    DuplicatePolicy.SKIP -> "Skip duplicates" to Icons.Rounded.Info
                    DuplicatePolicy.IMPORT_BOTH -> "Import both" to Icons.Rounded.AutoAwesome
                    DuplicatePolicy.REPLACE_EXISTING -> "Replace existing" to Icons.Rounded.Warning
                }

                val consequenceText = when (policy) {
                    DuplicatePolicy.SKIP -> "${preview.duplicates.size} existing transaction${if (preview.duplicates.size != 1) "s" else ""} will not be overwritten"
                    DuplicatePolicy.IMPORT_BOTH -> "Creates ${preview.duplicates.size} additional transaction${if (preview.duplicates.size != 1) "s" else ""} alongside existing ones"
                    DuplicatePolicy.REPLACE_EXISTING -> "${preview.duplicates.size} existing transaction${if (preview.duplicates.size != 1) "s" else ""} will be permanently updated"
                }
                val isSelected = duplicatePolicy == policy
                val consequenceColor = if (policy == DuplicatePolicy.REPLACE_EXISTING) AppColors.error else AppColors.contentSecondary

                Surface(
                    onClick = {
                        duplicatePolicy = policy
                        onSetDuplicatePolicy(policy)
                    },
                    color = if (isSelected) AppColors.accentContainer.copy(alpha = 0.15f) else AppColors.cardSurface,
                    shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
                    border = if (isSelected) BorderStroke(1.dp, AppColors.accent) else BorderStroke(0.5.dp, AppColors.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(AeonTheme.dimensions.cardPadding)
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                duplicatePolicy = policy
                                onSetDuplicatePolicy(policy)
                            }
                        )
                        Spacer(Modifier.width(AeonTheme.dimensions.spacingSm))
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) AppColors.accent else AppColors.contentSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(AeonTheme.dimensions.spacingSm))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                style = AppTextStyles.listItemTitle.copy(fontWeight = FontWeight.Bold),
                                color = if (isSelected) AppColors.contentPrimary else AppColors.contentSecondary
                            )
                            Text(
                                consequenceText,
                                style = AppTextStyles.caption,
                                color = if (isSelected) AppColors.contentPrimary.copy(alpha = 0.8f) else consequenceColor
                            )
                        }
                    }
                }
            }

            TextButton(onClick = { showDuplicates = !showDuplicates }) {
                Text(
                    if (showDuplicates) "Hide Duplicates" else "Show ${preview.duplicates.size} Duplicates",
                    style = AppTextStyles.buttonLabel,
                    color = AppColors.primary
                )
            }

            AnimatedVisibility(visible = showDuplicates) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    preview.duplicates.forEachIndexed { idx, dup ->
                        Surface(
                            color = AppColors.cardSurface,
                            shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = AeonTheme.dimensions.cardPadding, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = idx in selectedDuplicateRows,
                                    onCheckedChange = { checked ->
                                        if (checked) selectedDuplicateRows.add(idx)
                                        else selectedDuplicateRows.remove(idx)
                                        onToggleDuplicate(idx)
                                    },
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(AeonTheme.dimensions.spacingMd))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Existing: ${dup.existingNote.ifBlank { "(no note)" }}",
                                        style = AppTextStyles.caption,
                                        color = AppColors.contentSecondary
                                    )
                                    Text(
                                        "Incoming row ${dup.incomingRowIndex}: ${dup.incomingValues.take(4).joinToString(" · ")}",
                                        style = AppTextStyles.caption,
                                        color = AppColors.contentPrimary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Parse errors
        if (preview.parseErrors.isNotEmpty()) {
            HorizontalDivider(color = AppColors.divider.copy(alpha = 0.5f))
            TextButton(onClick = { showErrors = !showErrors }) {
                Text(
                    if (showErrors) "Hide ${preview.parseErrors.size} Errors"
                    else "Show ${preview.parseErrors.size} Parse Errors",
                    style = AppTextStyles.buttonLabel,
                    color = AppColors.error
                )
            }
            AnimatedVisibility(visible = showErrors) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    preview.parseErrors.forEach { err ->
                        Text(
                            "Row ${err.rowIndex}: ${err.reason}",
                            style = AppTextStyles.caption,
                            color = AppColors.error
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(AeonTheme.dimensions.spacingLg))
        AeonDashedButton(
            onClick = onImport,
            text = if (isLoading) "Importing…" else "Import $effectiveCount transaction${if (effectiveCount != 1) "s" else ""}",
            icon = Icons.Rounded.CheckCircle,
            enabled = effectiveCount > 0 && !isLoading
        )
        Spacer(Modifier.height(AeonTheme.dimensions.spacingLg))
    }
}

// ─── Post-Import Result ────────────────────────────────────────────────────────

@Composable
private fun ImportResultScreen(
    result: ImportSummary,
    onViewTransactions: () -> Unit,
    onReviewMappings: () -> Unit,
    onBack: () -> Unit
) {
    val isFullSuccess = result.imported > 0 && result.errors.isEmpty()
    val isPartial = result.imported > 0 && result.errors.isNotEmpty()
    val isZero = result.imported == 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.screenBackground)
            .statusBarsPadding()
            .padding(AeonTheme.dimensions.screenPadding)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
    ) {
        Spacer(Modifier.height(AeonTheme.dimensions.spacingXl))

        val (iconName, iconTint, headline, subLine) = when {
            isFullSuccess -> Quad(
                "CheckCircle",
                AppColors.positive,
                "Import complete",
                "${result.imported} transaction${if (result.imported != 1) "s" else ""} added"
            )
            isPartial -> Quad(
                "Warning",
                WarningColor,
                "Import finished with issues",
                "${result.imported} added \u00b7 ${result.errors.size} failed"
            )
            else -> Quad(
                "Info",
                AppColors.contentSecondary,
                "Nothing was imported",
                if (result.skipped > 0) "All rows already exist in OssWallet"
                else "All rows failed to parse \u2014 check your column mappings"
            )
        }

        AeonHeroHeader(
            title = headline,
            subtitle = subLine,
            iconName = iconName,
            accentColorHex = String.format("#%06X", 0xFFFFFF and iconTint.toArgb()),
            isLeftAligned = false
        )

        if (result.skipped > 0 && !isZero) {
            Text(
                "${result.skipped} duplicate row${if (result.skipped != 1) "s" else ""} were skipped",
                style = AppTextStyles.caption,
                color = AppColors.contentSecondary
            )
        }

        if (result.transfersRerouted > 0) {
            Text(
                "${result.transfersRerouted} transfer${if (result.transfersRerouted != 1) "s" else ""} re-routed to outside-wallet",
                style = AppTextStyles.caption,
                color = AppColors.contentSecondary
            )
        }

        if (isPartial) {
            var showErrors by remember { mutableStateOf(false) }
            TextButton(onClick = { showErrors = !showErrors }) {
                Text(
                    if (showErrors) "Hide errors" else "Show ${result.errors.size} errors",
                    style = AppTextStyles.buttonLabel,
                    color = AppColors.error
                )
            }
            AnimatedVisibility(visible = showErrors) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    result.errors.forEach { err ->
                        Text("Row ${err.rowIndex}: ${err.reason}", style = AppTextStyles.caption, color = AppColors.error)
                    }
                }
            }
        }

        Spacer(Modifier.height(AeonTheme.dimensions.spacingLg))

        if (!isZero) {
            AeonDashedButton(
                onClick = onViewTransactions,
                text = "View transactions",
                icon = Icons.AutoMirrored.Rounded.ArrowForward
            )
        } else {
            AeonDashedButton(
                onClick = onReviewMappings,
                text = "Review mappings",
                icon = Icons.Rounded.AttachFile
            )
        }

        TextButton(onClick = onBack) {
            Text("Back to Settings", style = AppTextStyles.buttonLabel, color = AppColors.contentSecondary)
        }
    }
}

@Composable
private fun StepProgressIndicator(stepIndex: Int) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm)
        ) {
            repeat(4) { index ->
                val isActive = index == stepIndex
                val isCompleted = index < stepIndex
                val color = when {
                    isActive    -> AppColors.primary
                    isCompleted -> AppColors.primary.copy(alpha = 0.6f)
                    else        -> AppColors.outlineVariant
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .background(color, RoundedCornerShape(2.dp))
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = when (stepIndex) {
                0    -> "Step 1: Source App"
                1    -> "Step 2: Column Mapping"
                2    -> "Step 3: Accounts & Categories"
                else -> "Step 4: Review & Confirm"
            },
            style = AppTextStyles.overlineLabel,
            color = AppColors.contentSecondary
        )
    }
}

// Helper for grouping 4 values
data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
operator fun <A, B, C, D> Quad<A, B, C, D>.component1() = a
operator fun <A, B, C, D> Quad<A, B, C, D>.component2() = b
operator fun <A, B, C, D> Quad<A, B, C, D>.component3() = c
operator fun <A, B, C, D> Quad<A, B, C, D>.component4() = d
