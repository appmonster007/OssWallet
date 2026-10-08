package com.osscabinet.ossWallet.widget

/**
 * Plain (non-Glance) AppWidgetProvider for the Category Spending Trend widget — raw RemoteViews,
 * built by [CategorySpendingTrendRemoteViewsRenderer]. See [BaseWidgetProvider] for the shared
 * refresh/cleanup shape.
 */
class CategorySpendingTrendWidgetProvider : BaseWidgetProvider({ CategorySpendingTrendWidgetPrefsStore(it) })
