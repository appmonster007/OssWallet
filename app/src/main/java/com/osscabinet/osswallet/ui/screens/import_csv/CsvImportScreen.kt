package com.osscabinet.ossWallet.ui.screens.import_csv

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BrokenImage
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.model.import_csv.DuplicatePolicy
import com.osscabinet.ossWallet.domain.model.import_csv.EntityMapping
import com.osscabinet.ossWallet.domain.model.import_csv.ImportSourceApp
import com.osscabinet.ossWallet.domain.model.import_csv.OssWalletField
import com.osscabinet.ossWallet.ui.components.AeonDashedButton
import com.osscabinet.ossWallet.ui.components.AeonHazeProvider
import com.osscabinet.ossWallet.ui.components.AeonTopAppBar
import com.osscabinet.ossWallet.ui.components.AeonTopBarNavTitlePill
import com.osscabinet.ossWallet.ui.components.AeonTopBarSection
import com.osscabinet.ossWallet.ui.components.aeonHazeSource
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
    onNavigateBack: () -> Unit
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
        is CsvImportViewModel.Step.ColumnMapping -> 0
        is CsvImportViewModel.Step.EntityMapping -> 1
        is CsvImportViewModel.Step.Preview -> 2
    }

    // Full-screen result state replaces the wizard
    if (result != null) {
        ImportResultScreen(
            result = result!!,
            onViewTransactions = onNavigateBack,
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
                TextButton(onClick = { viewModel.clearError() }) { Text("OK") }
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
                        // Step progress indicator
                        LinearProgressIndicator(
                            progress = { (stepIndex + 1) / 3f },
                            modifier = Modifier.fillMaxWidth(),
                            color = AppColors.primary,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Step ${stepIndex + 1} of 3",
                            style = AppTextStyles.chipLabel,
                            color = AppColors.contentSecondary
                        )
                        Spacer(Modifier.height(AeonTheme.dimensions.spacingMd))

                        // Content with loading overlay
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Step content — dimmed while loading
                            Box(modifier = Modifier.alpha(if (isLoading) 0.4f else 1f)) {
                                when (step) {
                                    is CsvImportViewModel.Step.ColumnMapping -> ColumnMappingStep(
                                        session = session,
                                        fileLoadError = if (session == null) error else null,
                                        onLoadFile = { fileLauncher.launch("*/*") },
                                        onUpdateMapping = { field, col -> viewModel.updateMapping(field, col) },
                                        onApplyPreset = { viewModel.applyPreset(it) },
                                        onNext = { viewModel.proceedFromColumnMapping(context) },
                                        isLoading = isLoading
                                    )
                                    is CsvImportViewModel.Step.EntityMapping -> EntityMappingStep(
                                        preview = preview,
                                        accounts = accounts.map { it.id to it.name },
                                        categories = categories.map { it.id.toString() to it.name },
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

// ─── Step 1: Column Mapping ───────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColumnMappingStep(
    session: com.osscabinet.ossWallet.domain.model.import_csv.CsvImportSession?,
    fileLoadError: String?,
    onLoadFile: () -> Unit,
    onUpdateMapping: (OssWalletField, Int?) -> Unit,
    onApplyPreset: (ImportSourceApp) -> Unit,
    onNext: () -> Unit,
    isLoading: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
    ) {
        if (session == null) {
            // Empty state
            if (fileLoadError != null) {
                // Inline error state
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
                ) {
                    Spacer(Modifier.height(AeonTheme.dimensions.spacingXl))
                    Icon(
                        Icons.Rounded.BrokenImage,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = AppColors.error
                    )
                    Text(
                        "File could not be opened",
                        style = AppTextStyles.heroAmount,
                        color = AppColors.contentPrimary
                    )
                    Text(
                        "The file may have been moved or is not a valid CSV. Try choosing a different file.",
                        style = AppTextStyles.caption,
                        color = AppColors.contentSecondary
                    )
                    Spacer(Modifier.height(AeonTheme.dimensions.spacingMd))
                    AeonDashedButton(
                        onClick = onLoadFile,
                        text = "Try Another File",
                        icon = Icons.Rounded.AttachFile
                    )
                }
            } else {
                // Normal empty state
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
                ) {
                    Spacer(Modifier.height(AeonTheme.dimensions.spacingXl))
                    Icon(
                        Icons.Rounded.FileOpen,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = AppColors.contentSecondary
                    )
                    Text(
                        "Import your transactions",
                        style = AppTextStyles.heroAmount,
                        color = AppColors.contentPrimary
                    )
                    Text(
                        "Supports BudgetBakers (Wallet) exports and any CSV with a header row.",
                        style = AppTextStyles.caption,
                        color = AppColors.contentSecondary
                    )
                    // Supported apps chips (informational)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm),
                    ) {
                        listOf("BudgetBakers", "Money Manager", "Any CSV").forEach { label ->
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
                    Spacer(Modifier.height(AeonTheme.dimensions.spacingMd))
                    AeonDashedButton(
                        onClick = onLoadFile,
                        text = "Choose CSV File",
                        icon = Icons.Rounded.AttachFile
                    )
                }
            }
        } else {
            val isBudgetBakers = session.sourceApp == ImportSourceApp.BUDGET_BAKERS
            var customiseMappings by remember { mutableStateOf(false) }

            // Auto-detect banner
            if (isBudgetBakers) {
                Surface(
                    color = AppColors.primaryContainer,
                    shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius)
                ) {
                    Row(
                        modifier = Modifier.padding(AeonTheme.dimensions.cardPadding),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
                    ) {
                        Icon(
                            Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp).padding(top = 2.dp),
                            tint = AppColors.primary
                        )
                        Column {
                            Text(
                                "BudgetBakers file detected",
                                style = AppTextStyles.cardSectionTitle,
                                color = AppColors.onPrimaryContainer
                            )
                            Text(
                                "All columns have been mapped automatically. Review below or tap Continue.",
                                style = AppTextStyles.caption,
                                color = AppColors.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // Preset chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Preset:",
                    style = AppTextStyles.caption,
                    color = AppColors.contentSecondary
                )
                FilterChip(
                    selected = isBudgetBakers,
                    onClick = { onApplyPreset(ImportSourceApp.BUDGET_BAKERS) },
                    label = { Text("BudgetBakers") }
                )
                FilterChip(
                    selected = session.sourceApp == ImportSourceApp.GENERIC,
                    onClick = { onApplyPreset(ImportSourceApp.GENERIC) },
                    label = { Text("Generic") }
                )
            }

            // Preview table
            Text(
                "Preview — first ${session.previewRows.size} rows",
                style = AppTextStyles.formSectionLabel,
                color = AppColors.contentSecondary
            )
            Surface(
                color = AppColors.cardSurface,
                shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius)
            ) {
                Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    Column {
                        // Header row
                        Row(modifier = Modifier.background(AppColors.cardSurfaceRaised)) {
                            session.headers.forEach { header ->
                                Text(
                                    text = header,
                                    modifier = Modifier.width(120.dp).padding(horizontal = 8.dp, vertical = 6.dp),
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
                                        modifier = Modifier.width(120.dp).padding(horizontal = 8.dp, vertical = 6.dp),
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
            if (isBudgetBakers && !customiseMappings) {
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
                    Text("Customise mappings", style = AppTextStyles.caption)
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
                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = it },
                                modifier = Modifier.weight(1.5f)
                            ) {
                                OutlinedTextField(
                                    value = selectedText,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                    textStyle = AppTextStyles.caption,
                                    singleLine = true
                                )
                                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                    DropdownMenuItem(
                                        text = { Text("Skip") },
                                        onClick = {
                                            onUpdateMapping(field, null)
                                            expanded = false
                                        }
                                    )
                                    session.headers.forEachIndexed { index, header ->
                                        DropdownMenuItem(
                                            text = { Text(header) },  // no index prefix
                                            onClick = {
                                                onUpdateMapping(field, index)
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        // Helper text for ambiguous fields — only when not skipped
                        val helperText = when (field) {
                            OssWalletField.IS_TRANSFER -> "Identifies paired transfer rows (e.g. credit card payments)"
                            OssWalletField.WARRANTY_MONTHS -> "Number of months warranty remaining on a purchased item"
                            OssWalletField.LABELS -> "Comma or pipe-separated tags attached to the transaction"
                            else -> null
                        }
                        if (helperText != null && currentColIndex != null) {
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
}

// ─── Step 2: Entity Mapping ───────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntityMappingStep(
    preview: com.osscabinet.ossWallet.domain.model.import_csv.ImportPreview?,
    accounts: List<Pair<String, String>>,
    categories: List<Pair<String, String>>,
    onConfirmMappings: (List<EntityMapping>) -> Unit
) {
    if (preview == null) return

    val accountMappings = remember(preview.newAccounts) {
        mutableStateListOf(*preview.newAccounts.map {
            EntityMapping(it, null, EntityMapping.EntityType.ACCOUNT)
        }.toTypedArray())
    }
    val categoryMappings = remember(preview.newCategories) {
        mutableStateListOf(*preview.newCategories.map { cat ->
            EntityMapping(cat.name, null, EntityMapping.EntityType.CATEGORY)
        }.toTypedArray())
    }
    val parentSelections = remember(preview.newCategories) {
        androidx.compose.runtime.mutableStateMapOf<Int, String?>()
    }

    // Blocking condition: custom category with "Create New" selected but no parent chosen
    val hasUnresolvedCustomCategory = preview.newCategories.indices.any { i ->
        preview.newCategories[i].isCustomCategory &&
                categoryMappings.getOrNull(i)?.existingId == null &&
                parentSelections[i] == null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
    ) {
        // Stakes header
        Text(
            "Unrecognised accounts & categories",
            style = AppTextStyles.heroAmount,
            color = AppColors.contentPrimary
        )
        Text(
            "These items appear in your file but don't exist in OssWallet. Map them to an existing item to keep your data organised, or leave as \"Create New\" to add them as new items.",
            style = AppTextStyles.caption,
            color = AppColors.contentSecondary
        )

        if (preview.newAccounts.isNotEmpty()) {
            Spacer(Modifier.height(AeonTheme.dimensions.spacingSm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "ACCOUNTS — ${preview.newAccounts.size} new",
                    style = AppTextStyles.formSectionLabel,
                    color = AppColors.contentSecondary
                )
                Row {
                    TextButton(onClick = { accountMappings.indices.forEach { i -> accountMappings[i] = accountMappings[i].copy(existingId = null) } }) {
                        Text("Create all new", style = AppTextStyles.caption)
                    }
                }
            }
            preview.newAccounts.forEachIndexed { index, name ->
                EntityMappingRow(
                    label = name,
                    badgeText = null,
                    chipText = null,
                    options = accounts,
                    selectedId = accountMappings.getOrNull(index)?.existingId,
                    onSelect = { id -> accountMappings[index] = accountMappings[index].copy(existingId = id) }
                )
            }
        }

        if (preview.newCategories.isNotEmpty()) {
            Spacer(Modifier.height(AeonTheme.dimensions.spacingSm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "CATEGORIES — ${preview.newCategories.size} new",
                    style = AppTextStyles.formSectionLabel,
                    color = AppColors.contentSecondary
                )
                Row {
                    TextButton(onClick = { categoryMappings.indices.forEach { i -> categoryMappings[i] = categoryMappings[i].copy(existingId = null) } }) {
                        Text("Create all new", style = AppTextStyles.caption)
                    }
                }
            }
            preview.newCategories.forEachIndexed { index, category ->
                Column {
                    EntityMappingRow(
                        label = category.name,
                        badgeText = null,
                        chipText = category.envelopeId?.let { "#$it" },
                        options = categories,
                        selectedId = categoryMappings.getOrNull(index)?.existingId,
                        onSelect = { id -> categoryMappings[index] = categoryMappings[index].copy(existingId = id) }
                    )
                    // Parent category picker for custom categories when "Create New" is selected
                    if (category.isCustomCategory && categoryMappings.getOrNull(index)?.existingId == null) {
                        val parentId = parentSelections[index]
                        val parentName = parentId?.let { id -> categories.find { it.first == id }?.second }
                        Surface(
                            color = AppColors.cardSurfaceRaised,
                            shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
                            modifier = Modifier.padding(start = AeonTheme.dimensions.spacingLg, top = 4.dp)
                        ) {
                            EntityMappingRow(
                                label = "Parent category (required)",
                                badgeText = null,
                                chipText = null,
                                options = categories,
                                selectedId = parentId,
                                onSelect = { id -> parentSelections[index] = id },
                                isError = parentId == null
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(AeonTheme.dimensions.spacingLg))
        AeonDashedButton(
            onClick = { onConfirmMappings(accountMappings.toList() + categoryMappings.toList()) },
            text = "Continue to Preview",
            icon = Icons.AutoMirrored.Rounded.ArrowForward,
            enabled = !hasUnresolvedCustomCategory
        )
        Spacer(Modifier.height(AeonTheme.dimensions.spacingLg))
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
    isError: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = selectedId?.let { id -> options.find { it.first == id }?.second } ?: "Create New"

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
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
                modifier = Modifier.weight(1.4f)
            ) {
                OutlinedTextField(
                    value = selectedName,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    textStyle = AppTextStyles.caption,
                    singleLine = true,
                    isError = isError
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Create New") },
                        onClick = { onSelect(null); expanded = false }
                    )
                    options.forEach { (id, name) ->
                        DropdownMenuItem(
                            text = { Text(name) },
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
    preview: com.osscabinet.ossWallet.domain.model.import_csv.ImportPreview?,
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
        // Summary card — 2×2 stat grid
        Surface(
            color = AppColors.cardSurface,
            shape = RoundedCornerShape(AeonTheme.dimensions.cardCornerRadius)
        ) {
            Column(modifier = Modifier.padding(AeonTheme.dimensions.cardPadding)) {
                Text(
                    "Import Summary",
                    style = AppTextStyles.cardSectionTitle,
                    color = AppColors.contentPrimary
                )
                Spacer(Modifier.height(AeonTheme.dimensions.spacingMd))
                Row(modifier = Modifier.fillMaxWidth()) {
                    // Top-left: hero — effective import count
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            effectiveCount.toString(),
                            style = AppTextStyles.heroAmount.copy(fontWeight = FontWeight.Bold),
                            color = AppColors.contentPrimary
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
                Spacer(Modifier.height(AeonTheme.dimensions.spacingSm))
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
                val consequenceText = when (policy) {
                    DuplicatePolicy.SKIP -> "${preview.duplicates.size} existing transaction${if (preview.duplicates.size != 1) "s" else ""} will not be overwritten"
                    DuplicatePolicy.IMPORT_BOTH -> "Creates ${preview.duplicates.size} additional transaction${if (preview.duplicates.size != 1) "s" else ""} alongside existing ones"
                    DuplicatePolicy.REPLACE_EXISTING -> "${preview.duplicates.size} existing transaction${if (preview.duplicates.size != 1) "s" else ""} will be permanently updated"
                }
                val consequenceColor = if (policy == DuplicatePolicy.REPLACE_EXISTING) AppColors.error else AppColors.contentSecondary
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = duplicatePolicy == policy,
                        onClick = {
                            duplicatePolicy = policy
                            onSetDuplicatePolicy(policy)
                        }
                    )
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        Text(
                            text = when (policy) {
                                DuplicatePolicy.SKIP -> "Skip duplicates"
                                DuplicatePolicy.IMPORT_BOTH -> "Import both"
                                DuplicatePolicy.REPLACE_EXISTING -> "Replace existing"
                            },
                            style = AppTextStyles.listItemTitle,
                            color = AppColors.contentPrimary
                        )
                        Text(
                            consequenceText,
                            style = AppTextStyles.caption,
                            color = consequenceColor
                        )
                    }
                }
            }

            TextButton(onClick = { showDuplicates = !showDuplicates }) {
                Text(if (showDuplicates) "Hide Duplicates" else "Show ${preview.duplicates.size} Duplicates")
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
            icon = if (isLoading) Icons.Rounded.CheckCircle else Icons.Rounded.CheckCircle,
            enabled = effectiveCount > 0 && !isLoading
        )
        Spacer(Modifier.height(AeonTheme.dimensions.spacingLg))
    }
}

// ─── Post-Import Result ────────────────────────────────────────────────────────

@Composable
private fun ImportResultScreen(
    result: com.osscabinet.ossWallet.domain.model.import_csv.ImportSummary,
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

        val (icon, iconTint, headline, subLine) = when {
            isFullSuccess -> Quad(
                Icons.Rounded.CheckCircle,
                AppColors.positive,
                "Import complete",
                "${result.imported} transaction${if (result.imported != 1) "s" else ""} added"
            )
            isPartial -> Quad(
                Icons.Rounded.Warning,
                WarningColor,
                "Import finished with issues",
                "${result.imported} added · ${result.errors.size} failed"
            )
            else -> Quad(
                Icons.Rounded.Info,
                AppColors.contentSecondary,
                "Nothing was imported",
                if (result.skipped > 0) "All rows already exist in OssWallet"
                else "All rows failed to parse — check your column mappings"
            )
        }

        Icon(icon, contentDescription = null, modifier = Modifier.size(64.dp), tint = iconTint)
        Text(headline, style = AppTextStyles.heroAmount, color = AppColors.contentPrimary)
        Text(subLine, style = AppTextStyles.listItemTitle, color = AppColors.contentSecondary)

        if (result.skipped > 0 && !isZero) {
            Text(
                "${result.skipped} duplicate row${if (result.skipped != 1) "s" else ""} were skipped",
                style = AppTextStyles.caption,
                color = AppColors.contentSecondary
            )
        }

        if (isPartial) {
            var showErrors by remember { mutableStateOf(false) }
            TextButton(onClick = { showErrors = !showErrors }) {
                Text(if (showErrors) "Hide errors" else "Show ${result.errors.size} errors", color = AppColors.error)
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
            Text("Back to Settings", style = AppTextStyles.caption, color = AppColors.contentSecondary)
        }
    }
}

// Helper for grouping 4 values without a data class
private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
private operator fun <A, B, C, D> Quad<A, B, C, D>.component1() = a
private operator fun <A, B, C, D> Quad<A, B, C, D>.component2() = b
private operator fun <A, B, C, D> Quad<A, B, C, D>.component3() = c
private operator fun <A, B, C, D> Quad<A, B, C, D>.component4() = d
