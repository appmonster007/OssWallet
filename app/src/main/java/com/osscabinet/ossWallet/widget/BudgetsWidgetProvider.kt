package com.osscabinet.ossWallet.widget

import com.osscabinet.ossWallet.R

/**
 * Plain (non-Glance) AppWidgetProvider for the Budgets widget — raw RemoteViews, built by
 * [BudgetsRemoteViewsRenderer]. See [BaseWidgetProvider] for the shared refresh/cleanup shape.
 */
class BudgetsWidgetProvider : BaseWidgetProvider(
    createPrefsStore = { BudgetsWidgetPrefsStore(it) },
    loadingLayoutResId = R.layout.widget_budgets_loading
)
