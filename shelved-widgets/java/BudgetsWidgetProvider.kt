package com.osscabinet.ossWallet.widget

/**
 * Plain (non-Glance) AppWidgetProvider for the Budgets widget — raw RemoteViews, built by
 * [BudgetsRemoteViewsRenderer]. See [BaseWidgetProvider] for the shared refresh/cleanup shape.
 */
class BudgetsWidgetProvider : BaseWidgetProvider({ BudgetsWidgetPrefsStore(it) })
