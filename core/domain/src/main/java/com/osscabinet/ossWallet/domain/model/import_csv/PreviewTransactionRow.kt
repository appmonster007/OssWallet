package com.osscabinet.ossWallet.domain.model.import_csv

/**
 * A row from [CsvImportSession.previewRows] rendered as it will roughly look once imported —
 * pure/no-I/O so it can back a real (non-mocked) sample-transactions preview in the UI.
 */
data class PreviewTransactionRow(
    val dateText: String,
    val accountText: String,
    val categoryText: String,
    val noteText: String,
    val amountText: String
)

private val genericHeaderGuesses = mapOf(
    OssWalletField.DATE to listOf("date", "datetime", "time"),
    OssWalletField.ACCOUNT to listOf("account", "wallet", "bank"),
    OssWalletField.CATEGORY to listOf("category", "category name", "cat"),
    OssWalletField.AMOUNT to listOf("amount", "value", "sum"),
    OssWalletField.NOTE to listOf("note", "title", "description", "memo")
)

private fun columnValue(
    field: OssWalletField,
    headers: List<String>,
    row: List<String>,
    mapping: Map<OssWalletField, Int?>
): String {
    val mappedIndex = mapping[field]
    if (mappedIndex != null && mappedIndex >= 0 && mappedIndex < row.size) return row[mappedIndex]
    val guess = genericHeaderGuesses[field].orEmpty()
        .firstNotNullOfOrNull { name -> headers.indexOf(name).takeIf { it >= 0 } }
    return guess?.let { row.getOrNull(it) } ?: ""
}

fun mapPreviewTransactionRows(session: CsvImportSession, limit: Int = 5): List<PreviewTransactionRow> =
    session.previewRows.take(limit).map { row ->
        PreviewTransactionRow(
            dateText = columnValue(OssWalletField.DATE, session.headers, row, session.mapping),
            accountText = columnValue(OssWalletField.ACCOUNT, session.headers, row, session.mapping),
            categoryText = columnValue(OssWalletField.CATEGORY, session.headers, row, session.mapping),
            noteText = columnValue(OssWalletField.NOTE, session.headers, row, session.mapping),
            amountText = columnValue(OssWalletField.AMOUNT, session.headers, row, session.mapping)
        )
    }
