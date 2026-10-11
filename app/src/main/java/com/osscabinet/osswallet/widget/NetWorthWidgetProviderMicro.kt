package com.osscabinet.ossWallet.widget

/**
 * Picker-only sibling of [NetWorthWidgetProvider] — distinct AppWidgetProvider registration
 * for the "Compact" (Micro) tier so the home-screen widget picker can show the Compact tier's
 * own preview thumbnail as a separate choice alongside Trend (Strip) and Card.
 */
class NetWorthWidgetProviderMicro : BaseWidgetProvider({ NetWorthWidgetPrefsStore(it) })
