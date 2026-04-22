package com.osscabinet.ossWallet.ui.components.navigation

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp

/**
 * ExpressiveNavigationBar - Material 3 NavigationBar with expressive bubble indicator
 *
 * This component uses Material 3 components to create a custom navigation bar with
 * a bubble indicator that covers both icon and text when selected. The bubble fades
 * in/out smoothly between tabs using Material 3 animation specs.
 *
 * The bubble is drawn behind the tab content to ensure proper layering, and uses
 * the Material 3 expressive color system for a beautiful appearance.
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
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            tabs.forEachIndexed { index, tab ->
                val selected = selectedTab == index
                val interactionSource = remember { MutableInteractionSource() }
                
                // Bubble fade animation
                val bubbleAlpha by animateFloatAsState(
                    targetValue = if (selected) 1f else 0f,
                    animationSpec = tween(
                        durationMillis = 300,
                        easing = FastOutSlowInEasing
                    ),
                    label = "bubbleAlpha_$index"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    // Bubble background (drawn behind content)
                    Box(
                        modifier = Modifier
                            .height(48.dp)
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = bubbleAlpha)
                            )
                    )

                    // Tab content (drawn on top of bubble)
                    Row(
                        modifier = Modifier
                            .clickable(
                                onClick = { onTabSelected(index) },
                                interactionSource = interactionSource,
                                indication = null
                            )
                            .semantics {
                                contentDescription = tab.contentDescription
                                stateDescription = if (selected) "Selected" else "Not selected"
                                role = Role.Tab
                            }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (selected) tab.selectedIcon else tab.icon,
                            contentDescription = null,
                            tint = if (selected)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (selected) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * NavigationTab - Data class for navigation tab
 *
 * @param icon Icon to show when tab is not selected
 * @param selectedIcon Icon to show when tab is selected
 * @param label Text label for the tab
 * @param contentDescription Content description for accessibility (defaults to label)
 */
data class NavigationTab(
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val label: String,
    val contentDescription: String = label
)
