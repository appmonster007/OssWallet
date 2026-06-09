package com.osscabinet.ossWallet.ui.screens.import_csv

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.model.import_csv.DuplicatePolicy
import com.osscabinet.ossWallet.domain.model.import_csv.EntityMapping
import com.osscabinet.ossWallet.domain.model.import_csv.ImportSourceApp
import com.osscabinet.ossWallet.domain.model.import_csv.OssWalletField
import com.osscabinet.ossWallet.ui.components.AeonHazeProvider
import com.osscabinet.ossWallet.ui.components.AeonTopAppBar
import com.osscabinet.ossWallet.ui.components.AeonTopBarNavTitlePill
import com.osscabinet.ossWallet.ui.components.AeonTopBarSection
import com.osscabinet.ossWallet.ui.components.aeonHazeSource
import com.osscabinet.ossWallet.ui.theme.AeonSize
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
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

    if (result != null) {
        val importResult = result!!
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Import Complete") },
            text = {
                Column {
                    Text("Imported: ${importResult.imported} transactions")
                    Text("Skipped: ${importResult.skipped}")
                    if (importResult.errors.isNotEmpty()) {
                        Text("Errors: ${importResult.errors.size}", color = AppColors.error)
                    }
                }
            },
            confirmButton = {
                Button(onClick = onNavigateBack) { Text("Done") }
            }
        )
    }

    if (error != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Error") },
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
                        // Step progress
                        LinearProgressIndicator(
                            progress = { (stepIndex + 1) / 3f },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Step ${stepIndex + 1} of 3",
                            style = MaterialTheme.typography.labelSmall,
                            color = AppColors.contentSecondary
                        )
                        Spacer(Modifier.height(AeonTheme.dimensions.spacingMd))

                        if (isLoading) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        } else {
                            when (step) {
                                is CsvImportViewModel.Step.ColumnMapping -> ColumnMappingStep(
                                    session = session,
                                    onLoadFile = { fileLauncher.launch("*/*") },
                                    onUpdateMapping = { field, col -> viewModel.updateMapping(field, col) },
                                    onApplyPreset = { viewModel.applyPreset(it) },
                                    onNext = { viewModel.proceedFromColumnMapping(context) }
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
                                    onImport = { viewModel.executeImport(context) }
                                )
                            }
                        }
                    }
                }
            }

            AeonTopAppBar(
                left = AeonTopBarSection(
                    content = {
                        AeonTopBarNavTitlePill(
                            title = "Import CSV",
                            onBackClick = onNavigateBack
                        )
                    }
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColumnMappingStep(
    session: com.osscabinet.ossWallet.domain.model.import_csv.CsvImportSession?,
    onLoadFile: () -> Unit,
    onUpdateMapping: (OssWalletField, Int?) -> Unit,
    onApplyPreset: (ImportSourceApp) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
    ) {
        if (session == null) {
            Text(
                text = "Load a CSV file to begin mapping columns.",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.contentSecondary
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = onLoadFile) {
                androidx.compose.material3.Icon(Icons.Rounded.AttachFile, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Load File")
            }
        } else {
            // Preview rows table
            Text("Preview (first ${session.previewRows.size} rows)", style = MaterialTheme.typography.labelMedium, color = AppColors.contentPrimary)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                Column {
                    // Header row
                    Row {
                        session.headers.forEach { header ->
                            Text(
                                text = header,
                                modifier = Modifier.width(100.dp).padding(4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = AppColors.primary
                            )
                        }
                    }
                    // Data rows
                    session.previewRows.take(3).forEach { row ->
                        Row {
                            row.forEach { cell ->
                                Text(
                                    text = cell,
                                    modifier = Modifier.width(100.dp).padding(4.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppColors.contentSecondary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = AppColors.divider.copy(alpha = 0.5f))

            Text("Apply Preset", style = MaterialTheme.typography.labelMedium, color = AppColors.contentPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = session?.sourceApp == ImportSourceApp.BUDGET_BAKERS,
                    onClick = { onApplyPreset(ImportSourceApp.BUDGET_BAKERS) },
                    label = { Text("BudgetBakers") }
                )
                FilterChip(
                    selected = session?.sourceApp == ImportSourceApp.GENERIC,
                    onClick = { onApplyPreset(ImportSourceApp.GENERIC) },
                    label = { Text("Generic") }
                )
            }

            HorizontalDivider(color = AppColors.divider.copy(alpha = 0.5f))

            Text("Map Columns", style = MaterialTheme.typography.labelMedium, color = AppColors.contentPrimary)

            val columnOptions = listOf("Skip") + session.headers.mapIndexed { i, h -> "$i: $h" }

            OssWalletField.entries.forEach { field ->
                var expanded by remember { mutableStateOf(false) }
                val currentColIndex = session.mapping[field]
                val selectedText = if (currentColIndex != null && currentColIndex < session.headers.size) {
                    "$currentColIndex: ${session.headers[currentColIndex]}"
                } else "Skip"

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = field.displayName,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
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
                            textStyle = MaterialTheme.typography.bodySmall,
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
                                    text = { Text("$index: $header") },
                                    onClick = {
                                        onUpdateMapping(field, index)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
                Text("Next")
            }
        }
    }
}

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
        mutableStateListOf(*preview.newAccounts.map { EntityMapping(it, null, EntityMapping.EntityType.ACCOUNT) }.toTypedArray())
    }
    val categoryMappings = remember(preview.newCategories) {
        mutableStateListOf(*preview.newCategories.map { cat -> EntityMapping(cat.name, null, EntityMapping.EntityType.CATEGORY) }.toTypedArray())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
    ) {
        Text(
            "Map Unknown Accounts & Categories",
            style = MaterialTheme.typography.titleMedium,
            color = AppColors.contentPrimary
        )
        Text(
            "These accounts/categories were found in your CSV but don't exist yet. Map them to existing ones or leave blank to create new.",
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.contentSecondary
        )

        if (preview.newAccounts.isNotEmpty()) {
            Text("Unknown Accounts", style = MaterialTheme.typography.labelMedium, color = AppColors.contentPrimary)
            preview.newAccounts.forEachIndexed { index, name ->
                var expanded by remember { mutableStateOf(false) }
                val mapping = accountMappings.getOrNull(index)
                val selectedName = mapping?.existingId?.let { id -> accounts.find { it.first == id }?.second } ?: "Create New"

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = AppColors.contentPrimary)
                    Spacer(Modifier.width(8.dp))
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                        modifier = Modifier.weight(1.5f)
                    ) {
                        OutlinedTextField(
                            value = selectedName,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            DropdownMenuItem(text = { Text("Create New") }, onClick = {
                                accountMappings[index] = accountMappings[index].copy(existingId = null)
                                expanded = false
                            })
                            accounts.forEach { (id, accName) ->
                                DropdownMenuItem(text = { Text(accName) }, onClick = {
                                    accountMappings[index] = accountMappings[index].copy(existingId = id)
                                    expanded = false
                                })
                            }
                        }
                    }
                }
            }
        }

        if (preview.newCategories.isNotEmpty()) {
            Text("Unknown Categories", style = MaterialTheme.typography.labelMedium, color = AppColors.contentPrimary)
            // Local state for parent-category selection for custom categories
            val parentSelections = remember(preview.newCategories) {
                androidx.compose.runtime.mutableStateMapOf<Int, String?>()
            }
            preview.newCategories.forEachIndexed { index, category ->
                var expanded by remember { mutableStateOf(false) }
                var parentExpanded by remember { mutableStateOf(false) }
                val mapping = categoryMappings.getOrNull(index)
                val selectedName = mapping?.existingId?.let { id -> categories.find { it.first == id }?.second } ?: "Create New"
                val labelText = if (category.envelopeId != null) "${category.name} (#${category.envelopeId})" else category.name

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(labelText, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = AppColors.contentPrimary)
                        Spacer(Modifier.width(8.dp))
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = it },
                            modifier = Modifier.weight(1.5f)
                        ) {
                            OutlinedTextField(
                                value = selectedName,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodySmall
                            )
                            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                DropdownMenuItem(text = { Text("Create New") }, onClick = {
                                    categoryMappings[index] = categoryMappings[index].copy(existingId = null)
                                    expanded = false
                                })
                                categories.forEach { (id, catName) ->
                                    DropdownMenuItem(text = { Text(catName) }, onClick = {
                                        categoryMappings[index] = categoryMappings[index].copy(existingId = id)
                                        expanded = false
                                    })
                                }
                            }
                        }
                    }
                    if (category.isCustomCategory && mapping?.existingId == null) {
                        val selectedParentName = parentSelections[index]
                            ?.let { id -> categories.find { it.first == id }?.second } ?: "No Parent"
                        Spacer(Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(start = 8.dp)
                        ) {
                            Text(
                                "Parent category:",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.labelSmall,
                                color = AppColors.contentSecondary
                            )
                            Spacer(Modifier.width(8.dp))
                            ExposedDropdownMenuBox(
                                expanded = parentExpanded,
                                onExpandedChange = { parentExpanded = it },
                                modifier = Modifier.weight(1.5f)
                            ) {
                                OutlinedTextField(
                                    value = selectedParentName,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(parentExpanded) },
                                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodySmall
                                )
                                ExposedDropdownMenu(expanded = parentExpanded, onDismissRequest = { parentExpanded = false }) {
                                    DropdownMenuItem(text = { Text("No Parent") }, onClick = {
                                        parentSelections[index] = null
                                        parentExpanded = false
                                    })
                                    categories.forEach { (id, catName) ->
                                        DropdownMenuItem(text = { Text(catName) }, onClick = {
                                            parentSelections[index] = id
                                            parentExpanded = false
                                        })
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { onConfirmMappings(accountMappings.toList() + categoryMappings.toList()) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Next")
        }
    }
}

@Composable
private fun PreviewStep(
    preview: com.osscabinet.ossWallet.domain.model.import_csv.ImportPreview?,
    onSetDuplicatePolicy: (DuplicatePolicy) -> Unit,
    onToggleDuplicate: (Int) -> Unit,
    onImport: () -> Unit
) {
    if (preview == null) return

    var duplicatePolicy by remember { mutableStateOf(DuplicatePolicy.SKIP) }
    var showDuplicates by remember { mutableStateOf(false) }
    var showErrors by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
    ) {
        // Summary card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AeonTheme.dimensions.cardPadding)
        ) {
            Text("Import Summary", style = MaterialTheme.typography.titleMedium, color = AppColors.contentPrimary)
            Spacer(Modifier.height(8.dp))
            Text("Total rows: ${preview.totalRows}", style = MaterialTheme.typography.bodyMedium, color = AppColors.contentPrimary)
            Text("Valid rows: ${preview.validRows}", style = MaterialTheme.typography.bodyMedium, color = AppColors.contentPrimary)
            Text("Duplicates: ${preview.duplicates.size}", style = MaterialTheme.typography.bodyMedium, color = AppColors.contentSecondary)
            if (preview.parseErrors.isNotEmpty()) {
                Text("Errors: ${preview.parseErrors.size}", style = MaterialTheme.typography.bodyMedium, color = AppColors.error)
            }
            if (preview.unpairedTransferRows > 0) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "⚠ ${preview.unpairedTransferRows} transfer(s) have no matching pair — will be imported as income/expense",
                    style = MaterialTheme.typography.bodySmall,
                    color = WarningColor
                )
            }
        }

        HorizontalDivider(color = AppColors.divider.copy(alpha = 0.5f))

        // Duplicate policy
        if (preview.duplicates.isNotEmpty()) {
            Text("Duplicate Policy", style = MaterialTheme.typography.labelMedium, color = AppColors.contentPrimary)
            DuplicatePolicy.entries.forEach { policy ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = duplicatePolicy == policy,
                        onClick = {
                            duplicatePolicy = policy
                            onSetDuplicatePolicy(policy)
                        }
                    )
                    Text(
                        text = when (policy) {
                            DuplicatePolicy.SKIP -> "Skip duplicates"
                            DuplicatePolicy.IMPORT_BOTH -> "Import both"
                            DuplicatePolicy.REPLACE_EXISTING -> "Replace existing"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.contentPrimary
                    )
                }
            }

            TextButton(onClick = { showDuplicates = !showDuplicates }) {
                Text(if (showDuplicates) "Hide Duplicates" else "Show ${preview.duplicates.size} Duplicates")
            }

            if (showDuplicates) {
                preview.duplicates.forEach { dup ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text("Existing: ${dup.existingNote}", style = MaterialTheme.typography.bodySmall, color = AppColors.contentSecondary)
                        Text("Incoming row ${dup.incomingRowIndex}: ${dup.incomingValues.take(3).joinToString(", ")}", style = MaterialTheme.typography.bodySmall, color = AppColors.contentPrimary)
                        HorizontalDivider(color = AppColors.divider.copy(alpha = 0.3f))
                    }
                }
            }
        }

        // Errors
        if (preview.parseErrors.isNotEmpty()) {
            TextButton(onClick = { showErrors = !showErrors }) {
                Text(if (showErrors) "Hide Errors" else "Show ${preview.parseErrors.size} Errors", color = AppColors.error)
            }
            if (showErrors) {
                preview.parseErrors.forEach { err ->
                    Text("Row ${err.rowIndex}: ${err.reason}", style = MaterialTheme.typography.bodySmall, color = AppColors.error)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onImport,
            modifier = Modifier.fillMaxWidth(),
            enabled = preview.validRows > 0
        ) {
            androidx.compose.material3.Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Import ${preview.validRows} Rows")
        }
    }
}
