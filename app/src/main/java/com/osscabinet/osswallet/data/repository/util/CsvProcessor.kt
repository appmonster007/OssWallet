package com.osscabinet.ossWallet.data.repository.util

import java.io.File

object CsvProcessor {

    fun buildCsv(headers: List<String>, rows: List<List<Any?>>): String {
        val sb = StringBuilder()
        sb.append(headers.joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" }).append("\n")
        rows.forEach { row ->
            sb.append(row.joinToString(",") { 
                val str = it?.toString() ?: ""
                "\"${str.replace("\"", "\"\"")}\"" 
            }).append("\n")
        }
        return sb.toString()
    }

    fun parseCsv(file: File): List<Map<String, String>> {
        val lines = file.readLines()
        if (lines.isEmpty()) return emptyList()
        val headers = parseCsvLine(lines[0])
        return lines.drop(1).map { line ->
            val parts = parseCsvLine(line)
            headers.indices.associate { i -> headers[i] to (parts.getOrNull(i) ?: "") }
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val char = line[i]
            if (char == '"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                    current.append('"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (char == ',' && !inQuotes) {
                result.add(current.toString())
                current = StringBuilder()
            } else {
                current.append(char)
            }
            i++
        }
        result.add(current.toString())
        return result
    }
}
