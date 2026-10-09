package com.osscabinet.ossWallet.widget

import kotlinx.serialization.Serializable

/**
 * Per-widget-instance appearance override, shared by every widget's config. SYSTEM (the
 * default) just leaves every color/drawable in XML alone — those already resolve correctly via
 * Android's values/values-night resource-qualifier split, same as the rest of the app. LIGHT/DARK
 * force a specific look regardless of the device's current night mode, which requires actually
 * overriding colors at render time (see WidgetColors) — RemoteViews has no per-widget
 * configuration-qualifier mechanism the way Compose composition has, so "forced light on a dark
 * phone" can't be expressed by picking a different resource; the renderer has to set the color
 * explicitly.
 */
@Serializable
enum class WidgetTheme {
    SYSTEM, LIGHT, DARK
}
