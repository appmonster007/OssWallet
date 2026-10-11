package com.osscabinet.ossWallet.widget

/**
 * Plain (non-Glance) AppWidgetProvider for the NetWorth widget — raw RemoteViews, built by
 * [NetWorthRemoteViewsRenderer]. See [BaseWidgetProvider] for the shared refresh/cleanup shape.
 */
class NetWorthWidgetProvider : BaseWidgetProvider({ NetWorthWidgetPrefsStore(it) })
