package com.osscabinet.ossWallet.domain.model.import_csv

data class DuplicateGroup(
    val key: String,
    val existingNote: String,
    val incomingRowIndex: Int,
    val incomingValues: List<String>
)

data class ParseError(val rowIndex: Int, val reason: String)

data class ImportSummary(
    val imported: Int,
    val skipped: Int,
    val errors: List<ParseError>,
    val transfersRerouted: Int = 0,
    val batchId: String = ""
)

data class CategoryToMap(
    val name: String,
    val envelopeId: Int? = null,
    val isCustomCategory: Boolean = false,
    val suggestedParentName: String? = null
)

data class AccountToMap(
    val name: String,
    val suggestedExistingId: String?,
    val suggestedExistingName: String?,
    val rowCount: Int = 0,
    val hasDetectedCurrency: Boolean = false
)

data class ImportPreview(
    val totalRows: Int,
    val validRows: Int,
    val duplicates: List<DuplicateGroup>,
    val allCsvAccounts: List<AccountToMap>,
    val newCategories: List<CategoryToMap>,
    val parseErrors: List<ParseError>,
    val unpairedTransferRows: Int = 0,
    val totalDuplicateCount: Int = duplicates.size
)

enum class DuplicatePolicy { SKIP, IMPORT_BOTH, REPLACE_EXISTING }
enum class MissingEntityPolicy { AUTO_CREATE, SKIP }

data class EntityMapping(
    val unknownName: String,
    val existingId: String? = null,   // null = create new
    val entityType: EntityType,
    val parentCategoryId: Long? = null,    // existing DB parent ID
    val parentCategoryName: String? = null, // new-root parent (resolved at import time via categoryMap)
    val isExcluded: Boolean = false,
    val currencyCode: String? = null,      // for new account creation
    val allowedCurrencies: List<String> = emptyList(),
    val openingBalances: Map<String, String> = emptyMap(),
    val structureType: String? = null,      // AccountStructureType.name, accounts only
    val iconName: String? = null,
    val colorHex: String? = null,
    val nature: String? = null              // CategoryNature.name, categories only
) {
    enum class EntityType { ACCOUNT, CATEGORY }
}
