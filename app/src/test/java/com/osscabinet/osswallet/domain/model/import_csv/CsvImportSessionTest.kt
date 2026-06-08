package com.osscabinet.ossWallet.domain.model.import_csv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class CsvImportSessionTest {

    @Test
    fun `ossWalletField_allEntriesHaveDisplayName`() {
        OssWalletField.entries.forEach { field ->
            assertFalse(
                "OssWalletField.${field.name} has blank displayName",
                field.displayName.isBlank()
            )
        }
    }

    @Test
    fun `importSourceApp_allEntriesHaveDisplayName`() {
        ImportSourceApp.entries.forEach { app ->
            assertFalse(
                "ImportSourceApp.${app.name} has blank displayName",
                app.displayName.isBlank()
            )
        }
    }

    @Test
    fun `duplicatePolicy_hasThreeValues`() {
        assertEquals(3, DuplicatePolicy.entries.size)
    }

    @Test
    fun `duplicatePolicy_containsExpectedValues`() {
        val values = DuplicatePolicy.entries.map { it.name }.toSet()
        assert(DuplicatePolicy.SKIP.name in values)
        assert(DuplicatePolicy.IMPORT_BOTH.name in values)
        assert(DuplicatePolicy.REPLACE_EXISTING.name in values)
    }

    @Test
    fun `ossWalletField_hasExpectedEntryCount`() {
        assertEquals(11, OssWalletField.entries.size)
    }

    @Test
    fun `csvImportSession_defaultDelimiterIsComma`() {
        val session = CsvImportSession(
            headers = emptyList(),
            previewRows = emptyList(),
            mapping = emptyMap(),
            sourceApp = null
        )
        assertEquals(',', session.delimiter)
    }

    @Test
    fun `csvImportSession_semicolonDelimiterIsPreserved`() {
        val session = CsvImportSession(
            headers = listOf("account", "amount"),
            previewRows = emptyList(),
            mapping = emptyMap(),
            sourceApp = ImportSourceApp.BUDGET_BAKERS,
            delimiter = ';'
        )
        assertEquals(';', session.delimiter)
    }
}
