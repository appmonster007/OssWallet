package com.osscabinet.ossWallet.widget

/**
 * Plain (non-Glance) AppWidgetProvider for the Account Hero widget — raw RemoteViews, built by
 * [AccountHeroRemoteViewsRenderer]. See [BaseWidgetProvider] for the shared refresh/cleanup shape.
 */
class AccountHeroWidgetProvider : BaseWidgetProvider({ AccountHeroWidgetPrefsStore(it) })
