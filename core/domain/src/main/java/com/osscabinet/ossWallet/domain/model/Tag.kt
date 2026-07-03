package com.osscabinet.ossWallet.domain.model

enum class SystemTag(
    val id: Long,
    val tagName: String,
    val colorHex: Int
) {
    PLANNED(10000L, "Planned", -7829368);
}

data class Tag(
    val id: Long = 0,
    val name: String,
    val colorHex: Int = -1,
    val profileId: Int = 1
) {
    companion object {
        val SYSTEM_PLANNED = SystemTag.PLANNED.tagName
        val SYSTEM_PLANNED_ID = SystemTag.PLANNED.id
        val SYSTEM_TAG_COLOR = SystemTag.PLANNED.colorHex

        fun isSystemTag(id: Long) = SystemTag.entries.any { it.id == id }
        fun isSystemTag(name: String) = SystemTag.entries.any { it.tagName == name }
    }
}
