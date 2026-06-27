package com.osscabinet.ossWallet.domain.model.settings

enum class UiDensity(val displayName: String, val scale: Float) {
    SMALL("Small", 0.80f),
    MEDIUM("Medium", 0.90f),
    HIGH("High", 1.0f),
    EXTRA_HIGH("Extra High", 1.12f);

    companion object {
        val DEFAULT = HIGH
        fun fromName(name: String) = entries.find { it.name == name } ?: DEFAULT
    }
}
