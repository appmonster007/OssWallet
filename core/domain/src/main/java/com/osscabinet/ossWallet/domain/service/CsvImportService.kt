package com.osscabinet.ossWallet.domain.service

import com.osscabinet.ossWallet.domain.model.import_csv.CsvImportSession
import com.osscabinet.ossWallet.domain.model.import_csv.DuplicatePolicy
import com.osscabinet.ossWallet.domain.model.import_csv.EntityMapping
import com.osscabinet.ossWallet.domain.model.import_csv.ImportPreview
import com.osscabinet.ossWallet.domain.model.import_csv.ImportSummary
import java.io.InputStream

interface CsvImportService {
    suspend fun parseHeaders(inputStream: InputStream): CsvImportSession
    suspend fun generatePreview(session: CsvImportSession, inputStream: InputStream): ImportPreview
    suspend fun executeImport(
        session: CsvImportSession,
        inputStream: InputStream,
        duplicatePolicy: DuplicatePolicy,
        entityMappings: List<EntityMapping>,
        selectedDuplicateRowIndices: Set<Int>,
        onProgress: (Float) -> Unit = {}
    ): Result<ImportSummary>
}
