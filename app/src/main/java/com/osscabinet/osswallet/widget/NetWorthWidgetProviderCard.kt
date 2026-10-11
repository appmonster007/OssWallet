package com.osscabinet.ossWallet.widget

/**
 * Picker-only sibling of [NetWorthWidgetProvider] — a distinct AppWidgetProvider registration
 * so the home-screen widget picker can show the "Card" tier's own preview thumbnail as a
 * separate choice (Android only supports one `android:previewLayout` per provider, so showing
 * every tier in the picker requires one provider per tier; there is no single-widget way to do
 * it). Shares every bit of real behavior with [NetWorthWidgetProvider] — same fetcher,
 * renderer, prefs store, and config activity — and is still freely resizable across the full
 * range after placement, same as the others; only the starting size and picker thumbnail
 * differ. See res/xml/net_worth_widget_info_card.xml.
 */
class NetWorthWidgetProviderCard : BaseWidgetProvider({ NetWorthWidgetPrefsStore(it) })
