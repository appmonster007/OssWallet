package com.osscabinet.ossWallet.widget

/**
 * Plain (non-Glance) AppWidgetProvider for the Planned Payments widget — raw RemoteViews, built
 * by [PlannedPaymentsRemoteViewsRenderer]. See [BaseWidgetProvider] for the shared
 * refresh/cleanup shape. Confirm/Skip row actions are wired via
 * [PlannedPaymentsWidgetActionReceiver], a separate exported=false receiver.
 */
class PlannedPaymentsWidgetProvider : BaseWidgetProvider({ PlannedPaymentsWidgetPrefsStore(it) })
