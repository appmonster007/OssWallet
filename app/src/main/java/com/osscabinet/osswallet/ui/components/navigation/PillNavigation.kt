package com.osscabinet.ossWallet.ui.components.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.osscabinet.ossWallet.domain.model.Template
import com.osscabinet.ossWallet.ui.components.AeonFloatingActionButton
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.History

/**
 * NavigationTab - Data class for navigation tab
 */
data class NavigationTab(
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val label: String,
    val contentDescription: String = label
)

@Composable
fun UnifiedPillNavigation(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    tabs: List<NavigationTab>,
    modifier: Modifier = Modifier,
    fabPaddingBottom: Dp = 16.dp,
    showFab: Boolean = true,
    isFabExpanded: Boolean = false,
    onFabToggle: (Boolean) -> Unit = {},
    fabIcon: ImageVector = Icons.Default.Add,
    onFabClick: () -> Unit = {},
    fabMenu: @Composable (ColumnScope.() -> Unit)? = null
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Background dimming for FAB expansion
        if (fabMenu != null) {
            AnimatedVisibility(
                visible = isFabExpanded,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize().zIndex(1f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onFabToggle(false) }
                )
            }
        }

        // Floating Pill Navigation and FAB Assembly
        Row(
            modifier = modifier
                .wrapContentWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = fabPaddingBottom)
                .padding(horizontal = 16.dp)
                .zIndex(2f),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally) 
        ) {
            if (tabs.isNotEmpty()) {
                Box(
                    modifier = Modifier.wrapContentSize(),
                    contentAlignment = Alignment.Center
                ) {
                    PillNavigationBar(
                        selectedTab = selectedTab,
                        onTabSelected = onTabSelected,
                        tabs = tabs,
                        modifier = Modifier.widthIn(max = 200.dp)
                    )

                    // Animated Scrim covering the pill when FAB is expanded
                    val scrimAlpha by animateFloatAsState(
                        targetValue = if (isFabExpanded) 0.45f else 0f,
                        animationSpec = tween(durationMillis = 300),
                        label = "scrimAlpha"
                    )

                    if (scrimAlpha > 0f) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .graphicsLayer { alpha = scrimAlpha }
                                .background(
                                    color = Color.Black,
                                    shape = CircleShape
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { onFabToggle(false) }
                                .zIndex(1f)
                        )
                    }
                }
            }

            if (showFab) {
                Box(
                    modifier = Modifier.width(56.dp), // Fixed width for stability
                    contentAlignment = Alignment.BottomCenter
                ) {
                    if (fabMenu != null) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .padding(bottom = 72.dp, end = 8.dp) // End padding to align centers: (56-40)/2 = 8dp
                                .align(Alignment.BottomEnd)
                                .wrapContentWidth(align = Alignment.End, unbounded = true)
                        ) {
                            fabMenu()
                        }
                    }

                    val rotation by animateFloatAsState(
                        targetValue = if (isFabExpanded) 45f else 0f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "FabRotation"
                    )

                    FloatingActionButton(
                        onClick = {
                            if (fabMenu != null) {
                                onFabToggle(!isFabExpanded)
                            } else {
                                onFabClick()
                            }
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .graphicsLayer { rotationZ = rotation },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = CircleShape,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
                    ) {
                        Icon(
                            imageVector = fabIcon,
                            contentDescription = if (isFabExpanded) "Close" else "Add",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PillNavigationBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    tabs: List<NavigationTab>,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(56.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(12.dp),
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .padding(6.dp)
                .fillMaxHeight()
        ) {
            val tabWidth = maxWidth / tabs.size

            // Sliding Indicator
            val indicatorOffset by animateDpAsState(
                targetValue = tabWidth * selectedTab,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "indicatorOffset"
            )

            Box(
                modifier = Modifier
                    .offset { IntOffset(x = indicatorOffset.roundToPx(), y = 0) }
                    .padding(horizontal = 4.dp) // Subtle internal padding for the indicator
                    .fillMaxWidth(1f / tabs.size) // Precise width calculation
                    .fillMaxHeight()
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    )
            )

            Row(modifier = Modifier.fillMaxSize()) {
                tabs.forEachIndexed { index, tab ->
                    val selected = selectedTab == index
                    val interactionSource = remember { MutableInteractionSource() }
                    
                    var isPressed by remember { mutableStateOf(false) }
                    val scale by animateFloatAsState(
                        targetValue = if (isPressed) 0.92f else 1f,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "tabScale"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .clickable(
                                onClick = { onTabSelected(index) },
                                interactionSource = interactionSource,
                                indication = null
                            )
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        isPressed = event.type == PointerEventType.Press
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (selected) tab.selectedIcon else tab.icon,
                            contentDescription = tab.contentDescription,
                            tint = if (selected)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionFabMenu(
    isExpanded: Boolean,
    templates: List<Template>,
    onAddClick: (Int?, String?) -> Unit,
    onAddAccountClick: (() -> Unit)? = null
) {
    val displayTemplates = remember(templates) {
        val starred = templates.filter { it.isStarred }
        val finalTemplates = if (starred.isNotEmpty()) {
            starred
        } else if (templates.isNotEmpty()) {
            templates.take(5)
        } else {
            emptyList()
        }

        finalTemplates.map {
            Triple(it.name, when(it.transactionType) {
                1 -> Icons.Rounded.Payments
                2 -> Icons.Rounded.SwapHoriz
                else -> Icons.Rounded.ShoppingCart
            }, it.transactionType)
        }
    }

    displayTemplates.forEachIndexed { index, (name, icon, type) ->
        FabMenuItem(
            visible = isExpanded,
            label = "Template: $name",
            icon = icon,
            index = index,
            onClick = { onAddClick(type, name) }
        )
    }

    val baseIndex = displayTemplates.size
    if (onAddAccountClick != null) {
        FabMenuItem(
            visible = isExpanded,
            label = "Add Account",
            icon = Icons.Default.AccountBalance,
            index = baseIndex,
            onClick = { onAddAccountClick() }
        )
    }

    FabMenuItem(
        visible = isExpanded,
        label = "Transfer",
        icon = Icons.Default.SyncAlt,
        index = if (onAddAccountClick != null) baseIndex + 1 else baseIndex,
        onClick = { onAddClick(2, null) }
    )

    FabMenuItem(
        visible = isExpanded,
        label = "New Transaction",
        icon = Icons.Default.Add,
        index = if (onAddAccountClick != null) baseIndex + 2 else baseIndex + 1,
        onClick = { onAddClick(0, null) }
    )
}

@Composable
fun FabMenuItem(
    visible: Boolean,
    label: String,
    icon: ImageVector,
    index: Int,
    onClick: () -> Unit
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "menuItemScale"
    )

    val staggerDelay = index * 40
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { it / 2 },
            animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioLowBouncy)
        ) + fadeIn(animationSpec = tween(durationMillis = 150, delayMillis = staggerDelay)) +
                scaleIn(initialScale = 0.7f, animationSpec = tween(durationMillis = 200, delayMillis = staggerDelay)),
        exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut() + scaleOut(targetScale = 0.7f),
        modifier = Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .wrapContentSize(unbounded = true)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            isPressed = event.type == PointerEventType.Press
                        }
                    }
                }
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
                shape = RoundedCornerShape(20.dp),
                shadowElevation = if (isPressed) 1.dp else 4.dp,
                onClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            ) {
                Text(
                    label,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
            }
            SmallFloatingActionButton(
                onClick = onClick,
                modifier = Modifier.size(40.dp),
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 800)
@Composable
fun UnifiedPillNavigationPreview() {
    val tabs = listOf(
        NavigationTab(
            icon = Icons.Rounded.Dashboard,
            selectedIcon = Icons.Rounded.Dashboard,
            label = "Glance"
        ),
        NavigationTab(
            icon = Icons.Rounded.History,
            selectedIcon = Icons.Rounded.History,
            label = "Activities"
        )
    )
    OssWalletTheme {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            UnifiedPillNavigation(
                selectedTab = 0,
                onTabSelected = {},
                tabs = tabs,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 800)
@Composable
fun UnifiedPillNavigationExpandedPreview() {
    val tabs = listOf(
        NavigationTab(
            icon = Icons.Rounded.Dashboard,
            selectedIcon = Icons.Rounded.Dashboard,
            label = "Glance"
        ),
        NavigationTab(
            icon = Icons.Rounded.History,
            selectedIcon = Icons.Rounded.History,
            label = "Activities"
        )
    )
    OssWalletTheme {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            UnifiedPillNavigation(
                selectedTab = 0,
                onTabSelected = {},
                tabs = tabs,
                isFabExpanded = true,
                onFabToggle = {},
                fabMenu = {
                    TransactionFabMenu(
                        isExpanded = true,
                        templates = emptyList(),
                        onAddClick = { _, _ -> }
                    )
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
