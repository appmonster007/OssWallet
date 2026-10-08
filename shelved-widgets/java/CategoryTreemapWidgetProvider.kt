package com.osscabinet.ossWallet.widget

/**
 * Plain (non-Glance) AppWidgetProvider for the Category Treemap widget — raw RemoteViews, built
 * by [CategoryTreemapRemoteViewsRenderer]. See [BaseWidgetProvider] for the shared
 * refresh/cleanup shape.
 */
class CategoryTreemapWidgetProvider : BaseWidgetProvider({ CategoryTreemapWidgetPrefsStore(it) })
