package com.osscabinet.ossWallet.widget

/**
 * Plain (non-Glance) AppWidgetProvider for the Loans widget — raw RemoteViews, built by
 * [LoansRemoteViewsRenderer]. See [BaseWidgetProvider] for the shared refresh/cleanup shape.
 */
class LoansWidgetProvider : BaseWidgetProvider({ LoansWidgetPrefsStore(it) })
