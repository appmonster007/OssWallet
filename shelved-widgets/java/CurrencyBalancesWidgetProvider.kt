package com.osscabinet.ossWallet.widget

/**
 * Plain (non-Glance) AppWidgetProvider for the Currency Balances widget — raw RemoteViews,
 * built by [CurrencyBalancesRemoteViewsRenderer]. See [BaseWidgetProvider] for the shared
 * refresh/cleanup shape.
 */
class CurrencyBalancesWidgetProvider : BaseWidgetProvider({ CurrencyBalancesWidgetPrefsStore(it) })
