package com.osscabinet.ossWallet.domain.model.settings

/**
 * How a card reveals its configuration when opened.
 *
 * [FLIP] is the historical default — the whole card turns over to its back. The others keep the
 * card header fixed and swap only the body between the reading and its settings, resizing the card
 * to fit; they differ only in how the body leaves and arrives.
 */
enum class CardOpenAnimation(val displayName: String, val description: String) {
    FLIP("Flip", "The card turns over to reveal its settings"),
    DISSOLVE("Dissolve", "Fade out, resize the card, then fade the settings in"),
    ROLL("Roll", "The reading rolls up into the header; the settings roll down"),
    PAGE("Page", "The reading slides out; the settings slide in from the side"),
    DROP("Drop", "A settings curtain drops over the reading as the card expands");

    companion object {
        val DEFAULT = FLIP
        fun fromName(name: String) = entries.find { it.name == name } ?: DEFAULT
    }
}
