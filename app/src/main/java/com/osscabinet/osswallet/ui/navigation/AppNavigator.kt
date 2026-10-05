package com.osscabinet.ossWallet.ui.navigation

import androidx.compose.runtime.Stable
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder

/**
 * Single funnel for navigation intents, so NavOptions follow from the route's category instead of
 * being chosen — or forgotten — per call site. Navigation was ~95 raw `navController.navigate()`
 * calls in three inconsistent dialects, and the drawer's Settings / Account / Template / Planning
 * routes passed no options at all, so a rapid double-tap could push duplicate back stack entries.
 *
 * A [TopLevelRoute] is a sibling tab switch and saves/restores back stack state; anything else is
 * a hierarchical push. Both are single-top. Callers needing more pass [extraOptions].
 *
 * **Known scope gap:** only the drawer, the back handler and the widget deep link route through
 * this. The ~80 push-style `navigate()` calls in the `ui/navigation/graphs` package do not — they
 * don't exhibit the drawer bug class, and migrating them in one pass was judged too large a
 * regression surface. See CLAUDE.md.
 */
@Stable
class AppNavigator(private val navController: NavHostController) {

    fun navigateTo(route: Any, extraOptions: (NavOptionsBuilder.() -> Unit)? = null) {
        navController.navigate(route) {
            if (route is TopLevelRoute) {
                popUpTo<DashboardRoute> { saveState = true }
                restoreState = true
            }
            launchSingleTop = true
            extraOptions?.invoke(this)
        }
    }
}
