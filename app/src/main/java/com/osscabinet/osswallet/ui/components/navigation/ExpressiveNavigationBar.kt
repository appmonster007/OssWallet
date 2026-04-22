package com.osscabinet.ossWallet.ui.components.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * ExpressiveNavigationBar - Material 3 NavigationBar with expressive styling
 * 
 * This component follows Material 3 guidelines while adding expressive animations
 * and visual feedback. It's designed to be used as a bottomBar in Scaffold,
 * ensuring proper z-index handling with drawers and other modals.
 * 
 * @param selectedTab Currently selected tab index
 * @param onTabSelected Callback when a tab is selected
 * @param tabs List of tab data (icon and label)
 * @param modifier Modifier for the component
 */
@Composable
fun ExpressiveNavigationBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    tabs: List<NavigationTab>,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 8.dp
    ) {
        tabs.forEachIndexed { index, tab ->
            val selected = selectedTab == index
            
            NavigationBarItem(
                selected = selected,
                onClick = { 
                    scope.launch { onTabSelected(index) }
                },
                icon = {
                    ExpressiveNavigationBarIcon(
                        icon = if (selected) tab.selectedIcon else tab.icon,
                        selected = selected
                    )
                },
                label = { Text(tab.label) },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                    selectedTextColor = MaterialTheme.colorScheme.onPrimary,
                    indicatorColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.semantics {
                    contentDescription = "${tab.label} tab"
                    stateDescription = if (selected) "Selected" else "Not selected"
                    role = Role.Tab
                }
            )
        }
    }
}

/**
 * ExpressiveNavigationBarIcon - Icon with expressive animations
 */
@Composable
private fun ExpressiveNavigationBarIcon(
    icon: ImageVector,
    selected: Boolean
) {
    val iconSize by animateDpAsState(
        targetValue = if (selected) 26.dp else 24.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "iconSize"
    )
    
    val iconColor by animateColorAsState(
        targetValue = if (selected) 
            MaterialTheme.colorScheme.onPrimary 
        else 
            MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "iconColor"
    )

    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(iconSize),
        tint = iconColor
    )
}

/**
 * NavigationTab - Data class for navigation tab
 */
data class NavigationTab(
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val label: String,
    val contentDescription: String = label
)
