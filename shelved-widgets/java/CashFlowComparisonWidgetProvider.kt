package com.osscabinet.ossWallet.widget

/**
 * Plain (non-Glance) AppWidgetProvider for the Cash Flow Comparison widget — raw RemoteViews,
 * built by [CashFlowComparisonRemoteViewsRenderer]. See [BaseWidgetProvider] for the shared
 * refresh/cleanup shape.
 */
class CashFlowComparisonWidgetProvider : BaseWidgetProvider({ CashFlowComparisonWidgetPrefsStore(it) })
