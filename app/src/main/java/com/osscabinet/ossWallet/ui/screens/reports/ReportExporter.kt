package com.osscabinet.ossWallet.ui.screens.reports

import android.content.Context
import com.osscabinet.ossWallet.domain.service.report.ReportCsv
import com.osscabinet.ossWallet.domain.service.report.ReportData
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

enum class ReportFormat(val label: String, val extension: String, val mimeType: String) {
    PDF("PDF", "pdf", "application/pdf"),
    CSV("CSV", "csv", "text/csv")
}

/** Writes generated reports to the shared cache folder, from where they can be shared or copied out. */
@Singleton
class ReportExporter @Inject constructor(@ApplicationContext private val context: Context) {

    private val dir: File get() = File(context.cacheDir, "shared").apply { mkdirs() }

    suspend fun export(data: ReportData, format: ReportFormat): File = withContext(Dispatchers.IO) {
        purgeOld()
        val file = File(dir, fileName(data, format))
        when (format) {
            ReportFormat.PDF -> file.outputStream().buffered().use { ReportPdfWriter.write(data, it) }
            ReportFormat.CSV -> file.writeText(ReportCsv.render(data), Charsets.UTF_8)
        }
        file
    }

    /** Copies a generated report to a document the user picked with the system file dialog. */
    suspend fun saveTo(file: File, destinationUri: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val out = context.contentResolver.openOutputStream(android.net.Uri.parse(destinationUri), "wt")
                ?: return@runCatching false
            out.use { sink -> file.inputStream().use { it.copyTo(sink) } }
            true
        }.getOrDefault(false)
    }

    /** Reports hold financial detail, so don't let old ones pile up in the cache. */
    private fun purgeOld() {
        val cutoff = System.currentTimeMillis() - MAX_AGE_MS
        dir.listFiles()?.filter { it.name.startsWith(PREFIX) && it.lastModified() < cutoff }?.forEach { it.delete() }
    }

    internal companion object {
        const val PREFIX = "OssWallet_Report_"
        private const val MAX_AGE_MS = 60L * 60 * 1000

        fun fileName(data: ReportData, format: ReportFormat): String {
            val period = data.scope.periodLabel.replace(Regex("[^A-Za-z0-9]+"), "_").trim('_').take(40)
            return "$PREFIX${period}_${data.generatedAt}.${format.extension}"
        }
    }
}
