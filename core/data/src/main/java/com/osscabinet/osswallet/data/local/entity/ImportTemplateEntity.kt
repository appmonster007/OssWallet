package com.osscabinet.ossWallet.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** The last-used column mapping + dedupe policy for a given source app, offered as the default next time. */
@Entity(tableName = "import_templates")
data class ImportTemplateEntity(
    @PrimaryKey val sourceApp: String,
    val mappingJson: String,
    val duplicatePolicy: String,
    val updatedAt: Long
)
