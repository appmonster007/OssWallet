package com.osscabinet.ossWallet.domain.model.import_csv

enum class OssWalletField(val displayName: String) {
    DATE("Date"),
    ACCOUNT("Account"),
    CATEGORY("Category"),
    AMOUNT("Amount"),
    TYPE("Type (Income/Expense)"),
    NOTE("Note"),
    PAYEE("Payee"),
    PAYMENT_TYPE("Payment Method"),
    LABELS("Labels / Tags"),
    WARRANTY_MONTHS("Warranty (months)"),
    IS_TRANSFER("Is Transfer")
}

enum class ImportSourceApp(val displayName: String) {
    BUDGET_BAKERS("Wallet by BudgetBakers"),
    MONEY_MANAGER("Money Manager"),
    CASHEW("Cashew"),
    GENERIC("Generic CSV")
}

data class CsvImportSession(
    val headers: List<String>,
    val previewRows: List<List<String>>,
    val mapping: Map<OssWalletField, Int?>,
    val sourceApp: ImportSourceApp?,
    val delimiter: Char = ','
)
