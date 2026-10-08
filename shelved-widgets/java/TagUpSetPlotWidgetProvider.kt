package com.osscabinet.ossWallet.widget

/**
 * Plain (non-Glance) AppWidgetProvider for the Tag UpSet Plot widget — raw RemoteViews, built by
 * [TagUpSetPlotRemoteViewsRenderer]. See [BaseWidgetProvider] for the shared refresh/cleanup shape.
 */
class TagUpSetPlotWidgetProvider : BaseWidgetProvider({ TagUpSetPlotWidgetPrefsStore(it) })
