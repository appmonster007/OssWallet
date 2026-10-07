package com.osscabinet.ossWallet.widget

/**
 * Plain (non-Glance) AppWidgetProvider for the Cash Flow widget — raw RemoteViews, built by
 * [CashFlowRemoteViewsRenderer]. See [BaseWidgetProvider] for the shared refresh/cleanup shape.
 */
class CashFlowWidgetProvider : BaseWidgetProvider({ CashFlowWidgetPrefsStore(it) })
