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
    val errors: List<ParseError>
)

/**
 * Represents a category that could not be automatically matched to an existing OssWallet
 * category and requires manual mapping by the user.
 *
 * @param name The category name as it appears in the CSV (or mapped from envelope_id)
 * @param envelopeId The BudgetBakers numeric envelope ID, if present — shown as a hint
 * @param isCustomCategory True when the CSV row had custom_category=true (user-defined sub-category)
 */
data class CategoryToMap(
    val name: String,
    val envelopeId: Int? = null,
    val isCustomCategory: Boolean = false,
    val suggestedParentName: String? = null
)

data class ImportPreview(
    val totalRows: Int,
    val validRows: Int,
    val duplicates: List<DuplicateGroup>,
    val newAccounts: List<String>,
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
    val parentCategoryId: Long? = null  // for CATEGORY + create new: null = root
) {
    enum class EntityType { ACCOUNT, CATEGORY }
}
