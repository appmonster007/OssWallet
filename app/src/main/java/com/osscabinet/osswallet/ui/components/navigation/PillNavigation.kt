package com.osscabinet.ossWallet.ui.components.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Template
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.ui.components.getIconByName
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.ColorScrimDark
import com.osscabinet.ossWallet.ui.theme.ColorScrimPill
import com.osscabinet.ossWallet.ui.theme.GlassTokens
import com.osscabinet.ossWallet.ui.theme.LocalGlassmorphismEnabled
import com.osscabinet.ossWallet.ui.theme.LocalPillHazeState
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import dev.chrisbanes.haze.hazeEffect

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
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ColorScrimDark)
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
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = fabPaddingBottom)
                .padding(horizontal = 24.dp)
                .zIndex(2f),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (showFab) Arrangement.SpaceBetween else Arrangement.Center
        ) {
            // Invisible spacer to balance the FAB on the right, centering the Pill
            if (showFab) {
                Spacer(modifier = Modifier.width(56.dp))
            }

            if (tabs.isNotEmpty()) {
                Box(
                    modifier = Modifier.wrapContentSize(),
                    contentAlignment = Alignment.Center
                ) {
                    val navHeight = 56.dp
                    val internalPadding = 16.dp
                    val tabWidth = navHeight// Result: 44dp
                    val barWidth = (tabWidth * tabs.size) + (internalPadding * 3)

                    PillNavigationBar(
                        selectedTab = selectedTab,
                        onTabSelected = onTabSelected,
                        tabs = tabs,
                        modifier = Modifier.width(barWidth) // Fixes the width to match the height per tab
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
                                    color = ColorScrimPill,
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
                                .padding(
                                    bottom = 72.dp,
                                    end = 8.dp
                                ) // End padding to align centers: (56-40)/2 = 8dp
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
                        containerColor = AppColors.primary,
                        contentColor = AppColors.onPrimary,
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
    val hazeState = LocalPillHazeState.current
    val glassEnabled = LocalGlassmorphismEnabled.current
    val isDark = isSystemInDarkTheme()

    Box(modifier = modifier.height(56.dp)) {
        if (glassEnabled) {
            Spacer(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        shape = CircleShape
                        clip = false
                        shadowElevation = 8.dp.toPx()
                        ambientShadowColor = Color.Black.copy(alpha = 0.06f)
                        spotShadowColor = Color.Black.copy(alpha = 0.14f)
                    }
            )
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer { shape = CircleShape; clip = true }
                .background(AppColors.componentOnCard, CircleShape)
                .then(
                    if (glassEnabled && hazeState != null)
                        Modifier.hazeEffect(hazeState, GlassTokens.pillStyle())
                    else Modifier
                )
                .border(
                    0.5.dp,
                    if (isDark) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.55f),
                    CircleShape
                )
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
                        color = AppColors.primaryContainer,
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
                                AppColors.onSelectionContainer
                            else
                                AppColors.contentSecondary.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
        } // glass Box
    } // outer Box
}

@Composable
fun TransactionFabMenu(
    isExpanded: Boolean,
    templates: List<Template>,
    categories: List<Category> = emptyList(),
    onAddClick: (TransactionType?, Long?) -> Unit,
    onAddAccountClick: (() -> Unit)? = null
) {
    val categoryMap = remember(categories) { categories.associateBy { it.id } }

    val displayTemplates = remember(templates) {
        val starred = templates.filter { it.isStarred }
        if (starred.isNotEmpty()) starred else templates.take(5)
    }

    displayTemplates.forEachIndexed { index, template ->
        val icon = getIconByName(categoryMap[template.categoryId]?.iconName)
        FabMenuItem(
            visible = isExpanded,
            label = "Template: ${template.name}",
            icon = icon,
            index = index,
            onClick = { onAddClick(template.transactionType, template.id) }
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
        onClick = { onAddClick(TransactionType.TRANSFER, null) }
    )

    FabMenuItem(
        visible = isExpanded,
        label = "New Transaction",
        icon = Icons.Default.Add,
        index = if (onAddAccountClick != null) baseIndex + 2 else baseIndex + 1,
        onClick = { onAddClick(TransactionType.EXPENSE, null) }
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
            animationSpec = spring(
                stiffness = Spring.StiffnessLow,
                dampingRatio = Spring.DampingRatioLowBouncy
            )
        ) + fadeIn(animationSpec = tween(durationMillis = 150, delayMillis = staggerDelay)) +
                scaleIn(
                    initialScale = 0.7f,
                    animationSpec = tween(durationMillis = 200, delayMillis = staggerDelay)
                ),
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
                color = AppColors.cardSurfaceProminent,
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
                    style = AppTextStyles.drawerItemLabel,
                    color = AppColors.contentPrimary,
                    fontWeight = FontWeight.Medium
                )
            }
            SmallFloatingActionButton(
                onClick = onClick,
                modifier = Modifier.size(40.dp),
                containerColor = AppColors.secondaryContainer,
                contentColor = AppColors.onSecondaryContainer,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun UnifiedPillNavigationPreview() {
    val tabs = listOf(
        NavigationTab(Icons.Rounded.Dashboard, Icons.Rounded.Dashboard, "Dashboard"),
        NavigationTab(Icons.Rounded.History, Icons.Rounded.History, "History")
    )
    OssWalletTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            UnifiedPillNavigation(
                selectedTab = 0,
                onTabSelected = {},
                tabs = tabs,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
