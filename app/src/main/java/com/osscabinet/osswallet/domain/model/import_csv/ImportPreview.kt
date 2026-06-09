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
    val transfersRerouted: Int = 0
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
    val rowCount: Int = 0
)

data class ImportPreview(
    val totalRows: Int,
    val validRows: Int,
    val duplicates: List<DuplicateGroup>,
    val allCsvAccounts: List<AccountToMap>,
    val newCategories: List<CategoryToMap>,
    val parseErrors: List<ParseError>,
    val unpairedTransferRows: Int = 0
)

enum class DuplicatePolicy { SKIP, IMPORT_BOTH, REPLACE_EXISTING }
enum class MissingEntityPolicy { AUTO_CREATE, SKIP }

data class EntityMapping(
    val unknownName: String,
    val existingId: String? = null,  // null = create new
    val entityType: EntityType,
    val parentCategoryId: Long? = null,  // for CATEGORY + create new: null = root
    val isExcluded: Boolean = false
) {
    enum class EntityType { ACCOUNT, CATEGORY }
}
