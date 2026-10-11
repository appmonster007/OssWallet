package com.osscabinet.ossWallet.widget

/**
 * Plain (non-Glance) AppWidgetProvider for the Recent Transactions widget — raw RemoteViews,
 * built by [RecentTransactionsRemoteViewsRenderer]. See [BaseWidgetProvider] for the shared
 * refresh/cleanup shape.
 */
class RecentTransactionsWidgetProvider : BaseWidgetProvider({ RecentTransactionsWidgetPrefsStore(it) })
