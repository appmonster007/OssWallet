package com.osscabinet.ossWallet.widget

import com.osscabinet.ossWallet.R

/**
 * Plain (non-Glance) AppWidgetProvider for the Accounts widget — raw RemoteViews, built by
 * [AccountsRemoteViewsRenderer]. See [BaseWidgetProvider] for the shared refresh/cleanup shape.
 */
class AccountsWidgetProvider : BaseWidgetProvider(
    createPrefsStore = { AccountsWidgetPrefsStore(it) },
    loadingLayoutResId = R.layout.widget_accounts_loading
)
