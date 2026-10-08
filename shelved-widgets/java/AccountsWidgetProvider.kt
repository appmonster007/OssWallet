package com.osscabinet.ossWallet.widget

/**
 * Plain (non-Glance) AppWidgetProvider for the Accounts widget — raw RemoteViews, built by
 * [AccountsRemoteViewsRenderer]. See [BaseWidgetProvider] for the shared refresh/cleanup shape.
 */
class AccountsWidgetProvider : BaseWidgetProvider({ AccountsWidgetPrefsStore(it) })
