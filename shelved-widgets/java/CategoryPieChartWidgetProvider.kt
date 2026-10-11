package com.osscabinet.ossWallet.widget

/**
 * Plain (non-Glance) AppWidgetProvider for the Category Pie Chart widget — raw RemoteViews,
 * built by [CategoryPieChartRemoteViewsRenderer]. See [BaseWidgetProvider] for the shared
 * refresh/cleanup shape.
 */
class CategoryPieChartWidgetProvider : BaseWidgetProvider({ CategoryPieChartWidgetPrefsStore(it) })
