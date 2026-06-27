package com.osscabinet.ossWallet.domain.model.settings

/**
 * Visual identity of the app. H ("Editorial Instrument") is the default and only
 * implemented identity; A and B are reserved so the color/type seams can branch on
 * identity without a later refactor.
 */
enum class UiIdentity(val displayName: String) {
    H("Editorial Instrument"),
    A("Quiet Luxury"),
    B("Futuristic Instrument");

    companion object {
        val DEFAULT = H
        fun fromName(name: String) = entries.find { it.name == name } ?: DEFAULT
    }
}
