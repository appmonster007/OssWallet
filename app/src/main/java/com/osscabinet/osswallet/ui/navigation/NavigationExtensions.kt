package com.osscabinet.ossWallet.ui.navigation

import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import kotlinx.serialization.json.Json
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

fun NavHostController.navigateToRecords(
    filter: TransactionFilter,
    topLevel: Boolean = false,
    builder: (NavOptionsBuilder.() -> Unit)? = null
) {
    val encodedFilter = URLEncoder.encode(
        Json.encodeToString(filter),
        StandardCharsets.UTF_8.toString()
    )
    val route = TransactionListRoute(filterJson = encodedFilter, topLevel = topLevel)
    if (builder != null) navigate(route, builder) else navigate(route)
}

fun NavHostController.navigateToDashboard(
    context: DashboardViewContext = DashboardViewContext.None,
    topLevel: Boolean = false,
    builder: (NavOptionsBuilder.() -> Unit)? = null
) {
    val encodedContext = URLEncoder.encode(
        Json.encodeToString(context),
        StandardCharsets.UTF_8.toString()
    )
    val route = DashboardRoute(viewContextJson = encodedContext, topLevel = topLevel)
    if (builder != null) navigate(route, builder) else navigate(route)
}
