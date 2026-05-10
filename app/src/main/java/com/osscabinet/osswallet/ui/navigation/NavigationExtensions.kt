package com.osscabinet.ossWallet.ui.navigation

import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

import com.osscabinet.ossWallet.domain.model.TransactionFilter

/**
 * Extension functions for context-aware navigation
 * Provides type-safe navigation with filter/view context parameters
 */

/**
 * Navigate to TransactionListScreen with a filter
 * @param filter The filter to apply (encoded as JSON)
 * @param topLevel Whether to treat this as a top-level navigation (hamburger menu instead of back)
 * @param builder Optional navigation options builder
 */
fun NavHostController.navigateToRecords(
    filter: TransactionFilter,
    topLevel: Boolean = false,
    builder: (NavOptionsBuilder.() -> Unit)? = null
) {
    val encodedFilter = URLEncoder.encode(
        Json.encodeToString(filter),
        StandardCharsets.UTF_8.toString()
    )
    if (builder != null) {
        navigate(Screen.TransactionList.createRoute(encodedFilter, topLevel), builder)
    } else {
        navigate(Screen.TransactionList.createRoute(encodedFilter, topLevel))
    }
}

/**
 * Navigate to DashboardScreen with a view context
 * @param context The view context to apply (encoded as JSON)
 * @param topLevel Whether to treat this as a top-level navigation (hamburger menu instead of back)
 * @param builder Optional navigation options builder
 */
fun NavHostController.navigateToDashboard(
    context: DashboardViewContext,
    topLevel: Boolean = false,
    builder: (NavOptionsBuilder.() -> Unit)? = null
) {
    val encodedContext = URLEncoder.encode(
        Json.encodeToString(context),
        StandardCharsets.UTF_8.toString()
    )
    if (builder != null) {
        navigate(Screen.Dashboard.createRoute(encodedContext, topLevel), builder)
    } else {
        navigate(Screen.Dashboard.createRoute(encodedContext, topLevel))
    }
}

// Note: FilterStack navigation extensions removed as part of filter system cleanup
