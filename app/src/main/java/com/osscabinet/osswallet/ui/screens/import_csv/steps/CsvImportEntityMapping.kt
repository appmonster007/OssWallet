package com.osscabinet.ossWallet.ui.screens.import_csv.steps

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.CategoryNature
import com.osscabinet.ossWallet.domain.model.import_csv.EntityMapping
import com.osscabinet.ossWallet.domain.model.import_csv.ImportPreview
import com.osscabinet.ossWallet.domain.util.fuzzyMatch
import com.osscabinet.ossWallet.ui.components.AeonDashedButton
import com.osscabinet.ossWallet.ui.components.AeonSectionHeader
import com.osscabinet.ossWallet.ui.screens.categories.list.components.CategoryEditSheet
import com.osscabinet.ossWallet.ui.screens.import_csv.components.EntityMappingRow
import com.osscabinet.ossWallet.ui.screens.import_csv.components.ImportAccountConfigDialog
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.ThemePreviews
import com.osscabinet.ossWallet.ui.util.toHex

@Composable
fun EntityMappingStep(
    preview: ImportPreview?,
    accounts: List<Pair<String, String>>,
    categories: List<Pair<String, String>>,
    rootCategories: List<Pair<String, String>>,
    fullCategories: List<Category>,
    shortlistedCurrencies: List<String>,
    accountMappings: Map<String, EntityMapping>,
    categoryMappings: Map<String, EntityMapping>,
    parentGroupMappings: Map<String, String?>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSelectAllNewAccounts: () -> Unit,
    onUpdateAccountMapping: (String, EntityMapping) -> Unit,
    onUpdateCategoryMapping: (String, EntityMapping) -> Unit,
    onUpdateParentGroupMapping: (String, String?) -> Unit,
    onConfirmMappings: () -> Unit
) {
    if (preview == null) return

    val categoryGroups = remember(preview.newCategories) {
        preview.newCategories
            .mapIndexed { i, cat -> i to cat }
            .groupBy { (_, cat) -> cat.suggestedParentName ?: "" }
            .entries
            .map { (key, items) ->
                CategoryGroup(
                    key = key,
                    parentName = key.takeIf { it.isNotBlank() },
                    items = items
                )
            }
            .sortedWith(compareBy(nullsLast()) { it.parentName })
    }

    var activeCategoryGroupKey by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = activeCategoryGroupKey != null) {
        activeCategoryGroupKey = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
    ) {
        if (activeCategoryGroupKey == null) {
            MainEntityMappingContent(
                preview = preview,
                accounts = accounts,
                accountMappings = accountMappings,
                categoryGroups = categoryGroups,
                categoryMappings = categoryMappings,
                shortlistedCurrencies = shortlistedCurrencies,
                searchQuery = searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                onSelectAllNewAccounts = onSelectAllNewAccounts,
                onUpdateAccountMapping = onUpdateAccountMapping,
                onSelectGroup = { activeCategoryGroupKey = it.ifEmpty { "__ROOT__" } },
                onConfirm = onConfirmMappings
            )
        } else {
            val activeKey = activeCategoryGroupKey
            val group = categoryGroups.find { g ->
                (g.key.isEmpty() && activeKey == "__ROOT__") || g.key == activeKey
            }
            if (group != null) {
                GroupDetailView(
                    group = group,
                    categories = categories,
                    rootCategories = rootCategories,
                    fullCategories = fullCategories,
                    categoryMappings = categoryMappings,
                    parentGroupMappings = parentGroupMappings,
                    onUpdateCategoryMapping = onUpdateCategoryMapping,
                    onUpdateParentGroupMapping = onUpdateParentGroupMapping,
                    onBack = { activeCategoryGroupKey = null }
                )
            }
        }
    }
}

private data class CategoryGroup(
    val key: String,
    val parentName: String?,
    val items: List<Pair<Int, com.osscabinet.ossWallet.domain.model.import_csv.CategoryToMap>>
)

@Composable
private fun MainEntityMappingContent(
    preview: ImportPreview,
    accounts: List<Pair<String, String>>,
    accountMappings: Map<String, EntityMapping>,
    categoryGroups: List<CategoryGroup>,
    categoryMappings: Map<String, EntityMapping>,
    shortlistedCurrencies: List<String>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSelectAllNewAccounts: () -> Unit,
    onUpdateAccountMapping: (String, EntityMapping) -> Unit,
    onSelectGroup: (String) -> Unit,
    onConfirm: () -> Unit
) {
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

    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        placeholder = { Text("Search accounts & categories") },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        modifier = Modifier.fillMaxWidth()
    )

    val filteredAccounts = remember(preview.allCsvAccounts, searchQuery) {
        if (searchQuery.isBlank()) preview.allCsvAccounts
        else preview.allCsvAccounts.filter { it.name.fuzzyMatch(searchQuery) }
    }
    val filteredGroups = remember(categoryGroups, searchQuery) {
        if (searchQuery.isBlank()) categoryGroups
        else categoryGroups.filter { group ->
            (group.parentName?.fuzzyMatch(searchQuery) == true) ||
                group.items.any { (_, cat) -> cat.name.fuzzyMatch(searchQuery) }
        }
    }

    if (filteredAccounts.isNotEmpty()) {
        AccountsSection(
            filteredAccounts,
            accounts,
            accountMappings,
            shortlistedCurrencies,
            onSelectAllNewAccounts,
            onUpdateAccountMapping
        )
    }

    if (filteredGroups.isNotEmpty()) {
        CategoriesSection(filteredGroups, categoryMappings, onSelectGroup)
    }

    Spacer(Modifier.height(AeonTheme.dimensions.spacingLg))
    AeonDashedButton(
        onClick = onConfirm,
        text = "Continue to Preview",
        icon = Icons.AutoMirrored.Rounded.ArrowForward
    )
    Spacer(Modifier.height(AeonTheme.dimensions.spacingLg))
}

@Composable
private fun AccountsSection(
    csvAccounts: List<com.osscabinet.ossWallet.domain.model.import_csv.AccountToMap>,
    accounts: List<Pair<String, String>>,
    accountMappings: Map<String, EntityMapping>,
    shortlistedCurrencies: List<String>,
    onSelectAllNewAccounts: () -> Unit,
    onUpdateAccountMapping: (String, EntityMapping) -> Unit
) {
    var configuringAccount by remember {
        mutableStateOf<com.osscabinet.ossWallet.domain.model.import_csv.AccountToMap?>(null)
    }

    Spacer(Modifier.height(AeonTheme.dimensions.spacingSm))
    AeonSectionHeader(
        title = "ACCOUNTS — ${csvAccounts.size} found",
        trailing = {
            TextButton(onClick = onSelectAllNewAccounts) {
                Text("Select all new", style = AppTextStyles.caption, color = AppColors.primary)
            }
        }
    )
    csvAccounts.forEach { acc ->
        val em = accountMappings[acc.name] ?: EntityMapping(
            acc.name,
            acc.suggestedExistingId,
            EntityMapping.EntityType.ACCOUNT
        )
        val isExcluded = em.existingId == "__EXCLUDE__"
        val isConfigured = em.existingId == null && em.structureType != null
        EntityMappingRow(
            label = acc.name,
            badgeText = when {
                isExcluded -> "${acc.rowCount} rows skipped"
                isConfigured -> "configured ✓"
                else -> null
            },
            chipText = if (!isExcluded) acc.suggestedExistingName?.let { "matched: $it" } else null,
            options = accounts,
            selectedId = em.existingId,
            onSelect = { id ->
                if (id == null) {
                    configuringAccount = acc
                } else {
                    onUpdateAccountMapping(acc.name, em.copy(existingId = id))
                }
            },
            showExcludeOption = true,
            currencyCode = em.currencyCode,
            onCurrencySelect = { curr ->
                onUpdateAccountMapping(
                    acc.name,
                    em.copy(currencyCode = curr)
                )
            },
            supportedCurrencies = shortlistedCurrencies
        )
    }

    configuringAccount?.let { acc ->
        ImportAccountConfigDialog(
            csvName = acc.name,
            rowCount = acc.rowCount,
            initialPrimaryCurrency = accountMappings[acc.name]?.currencyCode ?: "INR",
            shortlistedCurrencies = shortlistedCurrencies,
            onDismiss = { configuringAccount = null },
            onSave = { mapping ->
                onUpdateAccountMapping(acc.name, mapping)
                configuringAccount = null
            }
        )
    }
}

@Composable
private fun CategoriesSection(
    categoryGroups: List<CategoryGroup>,
    categoryMappings: Map<String, EntityMapping>,
    onSelectGroup: (String) -> Unit
) {
    Spacer(Modifier.height(AeonTheme.dimensions.spacingSm))
    AeonSectionHeader(title = "CATEGORIES — Found new ones")

    categoryGroups.forEach { group ->
        val mappedCount = group.items.count { (_, cat) ->
            categoryMappings[cat.name]?.existingId != null
        }
        val allMapped = mappedCount == group.items.size

        Surface(
            onClick = { onSelectGroup(group.key) },
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
                        text = if (group.items.size == 1) "1 category" else "${group.items.size} categories",
                        style = AppTextStyles.caption,
                        color = AppColors.contentSecondary
                    )
                }
                Surface(
                    color = if (allMapped) AppColors.primaryContainer else AppColors.accentContainer.copy(
                        alpha = 0.3f
                    ),
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

@Composable
private fun GroupDetailView(
    group: CategoryGroup,
    categories: List<Pair<String, String>>,
    rootCategories: List<Pair<String, String>>,
    fullCategories: List<Category>,
    categoryMappings: Map<String, EntityMapping>,
    parentGroupMappings: Map<String, String?>,
    onUpdateCategoryMapping: (String, EntityMapping) -> Unit,
    onUpdateParentGroupMapping: (String, String?) -> Unit,
    onBack: () -> Unit
) {
    var configuringCategory by remember {
        mutableStateOf<com.osscabinet.ossWallet.domain.model.import_csv.CategoryToMap?>(null)
    }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        TextButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                tint = AppColors.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text("Back", style = AppTextStyles.buttonLabel, color = AppColors.primary)
        }
        Spacer(Modifier.weight(1f))
        Text(
            group.parentName ?: "Top-level",
            style = AppTextStyles.overlineLabel,
            color = AppColors.contentSecondary
        )
    }

    val parentName = group.parentName
    var resolvedParentId: Long? = null
    if (parentName != null) {
        AeonSectionHeader(title = "PARENT CATEGORY")
        val existingRootMatch =
            rootCategories.find { it.second.equals(parentName, ignoreCase = true) }
        val selectedParentId = parentGroupMappings[parentName] ?: existingRootMatch?.first
        resolvedParentId = selectedParentId?.toLongOrNull()

        EntityMappingRow(
            label = "\"$parentName\"",
            badgeText = null,
            chipText = existingRootMatch?.let { "matched: ${it.second}" },
            options = rootCategories,
            selectedId = selectedParentId,
            onSelect = { id -> onUpdateParentGroupMapping(parentName, id) }
        )
        Spacer(Modifier.height(AeonTheme.dimensions.spacingSm))
    }

    AeonSectionHeader(title = if (parentName != null) "SUB-CATEGORIES" else "CATEGORIES")
    group.items.forEach { (_, cat) ->
        val em = categoryMappings[cat.name] ?: EntityMapping(
            cat.name,
            null,
            EntityMapping.EntityType.CATEGORY
        )
        val isConfigured = em.existingId == null && em.iconName != null
        EntityMappingRow(
            label = cat.name,
            badgeText = if (isConfigured) "configured ✓" else null,
            chipText = cat.envelopeId?.let { "#$it" },
            options = categories,
            selectedId = em.existingId,
            onSelect = { id ->
                if (id == null) {
                    configuringCategory = cat
                } else {
                    onUpdateCategoryMapping(cat.name, em.copy(existingId = id))
                }
            }
        )
    }

    Spacer(Modifier.height(AeonTheme.dimensions.spacingLg))
    AeonDashedButton(
        onClick = onBack,
        text = "Done with this group",
        icon = Icons.Rounded.CheckCircle
    )
    Spacer(Modifier.height(AeonTheme.dimensions.spacingLg))

    configuringCategory?.let { cat ->
        val em = categoryMappings[cat.name]
        CategoryEditSheet(
            category = Category(
                id = 0,
                name = cat.name,
                iconName = em?.iconName ?: "Other",
                colorHex = em?.colorHex?.let {
                    runCatching { android.graphics.Color.parseColor(it) }.getOrNull()
                } ?: -1,
                isDefault = false,
                parentId = em?.parentCategoryId ?: resolvedParentId,
                nature = em?.nature?.let { runCatching { CategoryNature.valueOf(it) }.getOrNull() }
                    ?: CategoryNature.WANT
            ),
            parentCategories = fullCategories,
            onDismiss = { configuringCategory = null },
            onSave = { name, icon, colorHex, parentId, nature ->
                onUpdateCategoryMapping(
                    cat.name,
                    EntityMapping(
                        unknownName = cat.name,
                        existingId = null,
                        entityType = EntityMapping.EntityType.CATEGORY,
                        parentCategoryId = parentId,
                        parentCategoryName = if (parentId == null) parentName else null,
                        iconName = icon,
                        colorHex = colorHex.toHex(),
                        nature = nature.name
                    )
                )
                configuringCategory = null
            }
        )
    }
}

@ThemePreviews
@Composable
private fun EntityMappingStepPreview() {
    OssWalletTheme {
        Surface(color = AppColors.screenBackground) {
            EntityMappingStep(
                preview = ImportPreview(
                    totalRows = 10,
                    validRows = 8,
                    allCsvAccounts = listOf(
                        com.osscabinet.ossWallet.domain.model.import_csv.AccountToMap(
                            "HDFC CSV",
                            "1",
                            "HDFC Bank",
                            5,
                            true
                        ),
                        com.osscabinet.ossWallet.domain.model.import_csv.AccountToMap(
                            "Cash CSV",
                            null,
                            null,
                            3,
                            false
                        )
                    ),
                    newCategories = listOf(
                        com.osscabinet.ossWallet.domain.model.import_csv.CategoryToMap(
                            "Dining",
                            null,
                            true,
                            "Food"
                        ),
                        com.osscabinet.ossWallet.domain.model.import_csv.CategoryToMap(
                            "Uber",
                            null,
                            true,
                            "Transport"
                        )
                    ),
                    duplicates = emptyList(),
                    parseErrors = emptyList(),
                    unpairedTransferRows = 0
                ),
                accounts = listOf("1" to "HDFC Bank", "2" to "Cash"),
                categories = listOf("10" to "Food", "20" to "Transport"),
                rootCategories = listOf("10" to "Food", "20" to "Transport"),
                fullCategories = emptyList(),
                shortlistedCurrencies = listOf("INR", "USD"),
                accountMappings = emptyMap(),
                categoryMappings = emptyMap(),
                parentGroupMappings = emptyMap(),
                searchQuery = "",
                onSearchQueryChange = {},
                onSelectAllNewAccounts = {},
                onUpdateAccountMapping = { _, _ -> },
                onUpdateCategoryMapping = { _, _ -> },
                onUpdateParentGroupMapping = { _, _ -> },
                onConfirmMappings = {}
            )
        }
    }
}
