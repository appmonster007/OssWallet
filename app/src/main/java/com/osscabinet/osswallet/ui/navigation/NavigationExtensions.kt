package com.osscabinet.ossWallet.ui.navigation

import androidx.navigation.NavHostController
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Extension functions for context-aware navigation
 * Provides type-safe navigation with filter/view context parameters
 */

/**
 * Navigate to TransactionListScreen with a filter context
 * @param context The filter context to apply (encoded as JSON)
 */
fun NavHostController.navigateToRecords(context: RecordsFilterContext) {
    val encodedContext = URLEncoder.encode(
        Json.encodeToString(context),
        StandardCharsets.UTF_8.toString()
    )
    navigate(Screen.TransactionList.createRoute(encodedContext))
}

/**
 * Navigate to DashboardScreen with a view context
 * @param context The view context to apply (encoded as JSON)
 */
fun NavHostController.navigateToDashboard(context: DashboardViewContext) {
    val encodedContext = URLEncoder.encode(
        Json.encodeToString(context),
        StandardCharsets.UTF_8.toString()
    )
    navigate(Screen.Dashboard.createRoute(encodedContext))
}

// Note: FilterStack navigation extensions removed as part of filter system cleanup
