package com.osscabinet.ossWallet.widget

/**
 * Plain (non-Glance) AppWidgetProvider for the Income vs Expenses widget — raw RemoteViews,
 * built by [IncomeExpenseBarRemoteViewsRenderer]. See [BaseWidgetProvider] for the shared
 * refresh/cleanup shape.
 */
class IncomeExpenseBarWidgetProvider : BaseWidgetProvider({ IncomeExpenseBarWidgetPrefsStore(it) })
