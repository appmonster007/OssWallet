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

data class ImportPreview(
    val totalRows: Int,
    val validRows: Int,
    val duplicates: List<DuplicateGroup>,
    val newAccounts: List<String>,
    val newCategories: List<String>,
    val parseErrors: List<ParseError>
)

enum class DuplicatePolicy { SKIP, IMPORT_BOTH, REPLACE_EXISTING }
enum class MissingEntityPolicy { AUTO_CREATE, SKIP }

data class EntityMapping(
    val unknownName: String,
    val existingId: String? = null,  // null = create new
    val entityType: EntityType
) {
    enum class EntityType { ACCOUNT, CATEGORY }
}
