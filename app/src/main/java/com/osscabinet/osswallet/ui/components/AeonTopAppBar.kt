@file:OptIn(ExperimentalMaterial3Api::class)

package com.osscabinet.ossWallet.ui.components

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.osscabinet.ossWallet.ui.theme.AeonSize
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.GlassTokens
import com.osscabinet.ossWallet.ui.theme.HazeLayer
import com.osscabinet.ossWallet.ui.theme.LocalGlassmorphismEnabled
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import dev.chrisbanes.haze.hazeSource

/**
 * Standard configuration for a discrete section of the AeonTopAppBar.
 */
data class AeonTopBarSection(
    val content: @Composable () -> Unit,
    val visible: Boolean = true,
)

/**
 * Constants and Style Defaults for the Aeon TopAppBar.
 */
object AeonTopBarDefaults {
    val Height = AeonSize.D64.value
    val PillHeight = AeonSize.D48.value
    val HorizontalPadding = AeonSize.D12.value
    val ContentPadding = PaddingValues(AeonSize.D4.value)
    
    val TitleStyle: TextStyle
        @Composable get() = AppTextStyles.screenTitle.copy(
            fontSize = 16.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.sp
        )
    
    val SubtitleStyle: TextStyle
        @Composable get() = AppTextStyles.caption.copy(
            fontWeight = FontWeight.Normal,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            letterSpacing = 0.2.sp
        )

    @Composable
    fun windowInsets() = TopAppBarDefaults.windowInsets
}

/**
 * Base implementation of the Aeon "Floating Pill" top app bar.
 * Provides three explicit slots: left, center, and right.
 * Each section can be any Composable, allowing for extreme customization.
 */
@Composable
fun AeonTopAppBar(
    modifier: Modifier = Modifier,
    left: AeonTopBarSection? = null,
    center: AeonTopBarSection? = null,
    right: AeonTopBarSection? = null,
    windowInsets: WindowInsets = AeonTopBarDefaults.windowInsets(),
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(windowInsets)
            .height(AeonTopBarDefaults.Height)
            .padding(horizontal = AeonTopBarDefaults.HorizontalPadding)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AeonSize.D8.value)
        ) {
            // Left Section Slot
            Box(
                modifier = Modifier.wrapContentWidth(Alignment.Start),
                contentAlignment = Alignment.CenterStart
            ) {
                if (left != null && left.visible) {
                    left.content()
                }
            }

            // Center Section Slot
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                if (center != null && center.visible) {
                    center.content()
                }
            }

            // Right Section Slot
            Box(
                modifier = Modifier.wrapContentWidth(Alignment.End),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (right != null && right.visible) {
                    right.content()
                }
            }
        }
    }
}

/**
 * Standard container for TopBar sections.
 * Renders as a glass-blurred pill.
 */
@Composable
fun AeonTopBarPill(
    modifier: Modifier = Modifier,
    hazeLayer: Int = HazeLayer.Overlay,
    padding: PaddingValues = AeonTopBarDefaults.ContentPadding,
    contentColor: Color = AppColors.onScreenBackground,
    content: @Composable RowScope.() -> Unit
) {
    GlassSurface(
        shape = CircleShape,
        hazeLayer = hazeLayer,
        hazeStyle = GlassTokens.pillStyle(),
        fallbackColor = AppColors.componentOnCard,
        shadowElevation = AeonSize.D8.value,
        modifier = modifier.height(AeonTopBarDefaults.PillHeight)
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(padding).fillMaxHeight(),
                horizontalArrangement = Arrangement.spacedBy(AeonSize.D2.value),
                content = content
            )
        }
    }
}

/**
 * Standardized Left Section component combining Navigation and Title into a single pill.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AeonTopBarNavTitlePill(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBackClick: (() -> Unit)? = null,
    onMenuClick: (() -> Unit)? = null,
    isTopLevel: Boolean = onMenuClick != null,
    tabs: List<String> = emptyList(),
    selectedTabIndex: Int = 0,
    onTabSelected: (Int) -> Unit = {},
    titleContent: @Composable (() -> Unit)? = null,
    subtitleContent: @Composable (() -> Unit)? = null,
) {
    val hasNavigation = (isTopLevel && onMenuClick != null) || onBackClick != null

    AeonTopBarPill(
        modifier = modifier,
        padding = PaddingValues(
            start = if (hasNavigation) AeonSize.D4.value else AeonSize.D16.value,
            end = AeonSize.D16.value,
            top = AeonSize.D4.value,
            bottom = AeonSize.D4.value
        )
    ) {
        AeonTopBarNav(
            onBackClick = onBackClick,
            onMenuClick = onMenuClick,
            isTopLevel = isTopLevel
        )

        AeonTopBarTitle(
            title = title,
            subtitle = subtitle,
            tabs = tabs,
            selectedTabIndex = selectedTabIndex,
            onTabSelected = onTabSelected,
            titleContent = titleContent,
            subtitleContent = subtitleContent,
        )
    }
}

/**
 * Standardized Center Section component for search.
 */
@Composable
fun AeonTopBarSearchPill(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search...",
    onClear: (() -> Unit)? = null,
    focusRequester: FocusRequester = remember { FocusRequester() },
    onFocusChange: ((Boolean) -> Unit)? = null,
) {
    AeonTopBarPill(
        modifier = modifier.fillMaxWidth().height(AeonTopBarDefaults.PillHeight),
        padding = PaddingValues(horizontal = AeonSize.D12.value)
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            modifier = Modifier.size(AeonSize.D20.value),
            tint = AppColors.contentSecondary
        )
        
        Spacer(Modifier.width(AeonSize.D8.value))
        
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .onFocusChanged { onFocusChange?.invoke(it.isFocused) },
            singleLine = true,
            textStyle = AppTextStyles.transactionTitle.copy(
                color = AppColors.contentPrimary,
                fontSize = 14.sp
            ),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(
                            placeholder,
                            style = AppTextStyles.formFieldHint.copy(fontSize = 14.sp),
                            color = AppColors.contentSecondary.copy(alpha = 0.6f)
                        )
                    }
                    inner()
                }
            }
        )

        AnimatedVisibility(
            visible = query.isNotEmpty() && onClear != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            IconButton(
                onClick = { onClear?.invoke() },
                modifier = Modifier.size(AeonSize.D32.value)
            ) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = "Clear",
                    modifier = Modifier.size(AeonSize.D16.value),
                    tint = AppColors.contentSecondary
                )
            }
        }
    }
}

/**
 * Standardized Right Section component for actions.
 */
@Composable
fun AeonTopBarActionPill(
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit
) {
    AeonTopBarPill(modifier = modifier, content = actions)
}

/**
 * Standard Navigation component (Back or Menu icon).
 */
@Composable
fun AeonTopBarNav(
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
    onMenuClick: (() -> Unit)? = null,
    isTopLevel: Boolean = onMenuClick != null,
) {
    if (isTopLevel && onMenuClick != null) {
        AppBarIconButton(icon = Icons.Rounded.Menu, onClick = onMenuClick, modifier = modifier)
    } else if (onBackClick != null) {
        AppBarIconButton(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            onClick = onBackClick,
            modifier = modifier
        )
    }
}

/**
 * Navigation icon wrapped in a consistent pill.
 */
@Composable
fun AeonTopBarNavPill(
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
    onMenuClick: (() -> Unit)? = null,
    isTopLevel: Boolean = onMenuClick != null,
) {
    AeonTopBarPill(
        modifier = modifier,
        padding = PaddingValues(AeonSize.D4.value)
    ) {
        AeonTopBarNav(
            onBackClick = onBackClick,
            onMenuClick = onMenuClick,
            isTopLevel = isTopLevel
        )
    }
}

/**
 * Standard Title/Subtitle component with integrated tab navigation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AeonTopBarTitle(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    tabs: List<String> = emptyList(),
    selectedTabIndex: Int = 0,
    onTabSelected: (Int) -> Unit = {},
    titleContent: @Composable (() -> Unit)? = null,
    subtitleContent: @Composable (() -> Unit)? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    val isInteractive = tabs.size > 1
    val displaySubtitle = if (tabs.isNotEmpty()) tabs.getOrElse(selectedTabIndex) { "" } else subtitle

    Column(
        modifier = modifier
            .padding(vertical = AeonSize.D4.value)
            .then(if (isInteractive) Modifier.clickable { expanded = true } else Modifier),
        verticalArrangement = Arrangement.Center
    ) {
        if (titleContent != null) {
            titleContent()
        } else if (title.isNotEmpty()) {
            Text(
                title,
                style = AeonTopBarDefaults.TitleStyle,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.onScreenBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (subtitleContent != null || (displaySubtitle != null && displaySubtitle.isNotEmpty())) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (subtitleContent != null) {
                    subtitleContent()
                } else if (displaySubtitle != null) {
                    Text(
                        displaySubtitle,
                        style = AeonTopBarDefaults.SubtitleStyle,
                        color = AppColors.contentSecondary.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (isInteractive) {
                    Spacer(Modifier.width(AeonSize.D2.value))
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Switch Tab",
                        modifier = Modifier.size(AeonSize.D12.value),
                        tint = AppColors.contentSecondary.copy(alpha = 0.6f)
                    )
                }
            }
        }

        if (isInteractive) {
            AeonTopBarDropdown(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                options = tabs,
                selectedOptionIndex = selectedTabIndex,
                onOptionSelected = {
                    onTabSelected(it)
                    expanded = false
                }
            )
        }
    }
}

/**
 * Standardized interactive dropdown for the AeonTopAppBar.
 */
@Composable
fun AeonTopBarDropdown(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    options: List<String>,
    selectedOptionIndex: Int,
    onOptionSelected: (Int) -> Unit,
) {
    val cardSurface = AppColors.surfaceContainerLow
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()

    if (expanded) {
        Popup(
            onDismissRequest = onDismissRequest,
            properties = PopupProperties(focusable = true),
            offset = IntOffset(0, 100) // Positioned below the pill
        ) {
            GlassSurface(
                modifier = Modifier.width(IntrinsicSize.Max),
                shape = RoundedCornerShape(AeonSize.D16.value),
                hazeLayer = HazeLayer.Overlay,
                hazeStyle = GlassTokens.popupStyle(),
                fallbackColor = cardSurface,
                shadowElevation = AeonSize.D12.value,
                borderColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.40f),
            ) {
                Column(modifier = Modifier.padding(vertical = AeonSize.D8.value)) {
                    options.forEachIndexed { index, option ->
                        val isSelected = index == selectedOptionIndex
                        DropdownMenuItem(
                            text = {
                                Text(
                                    option,
                                    style = AppTextStyles.transactionTitle.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 14.sp
                                    )
                                )
                            },
                            onClick = { onOptionSelected(index) },
                            colors = MenuDefaults.itemColors(
                                textColor = if (isSelected) AppColors.primary else AppColors.contentPrimary
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppBarIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = AppColors.onScreenBackground
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(AeonSize.D40.value),
        enabled = enabled
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(AeonSize.D22.value),
            tint = if (enabled) tint else tint.copy(alpha = 0.38f)
        )
    }
}

/**
 * Interactive wrapper for Aeon Top Bar previews.
 * Provides toggles for Theme and Glass mode.
 */
@Composable
fun AeonTopBarInteractivePreviewWrapper(
    content: @Composable () -> Unit
) {
    var theme by remember { mutableStateOf("Dark") }
    var glassEnabled by remember { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Control Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1A1A1A))
                .padding(vertical = 8.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "PREVIEW MODE",
                style = TextStyle(
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            )
            
            Spacer(Modifier.weight(1f))

            // Theme Toggle
            PreviewToggleButton(
                text = theme.uppercase(),
                onClick = {
                    theme = when (theme) {
                        "Light" -> "Dark"
                        "Dark" -> "Amoled"
                        else -> "Light"
                    }
                }
            )

            // Glass Toggle
            PreviewToggleButton(
                text = if (glassEnabled) "GLASS: ON" else "GLASS: OFF",
                onClick = { glassEnabled = !glassEnabled }
            )
        }

        AeonTopAppBarPreview(theme = theme, glassEnabled = glassEnabled) {
            content()
        }
    }
}

@Composable
private fun PreviewToggleButton(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = TextStyle(color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "Standard Layout", group = "Aeon Top Bar", showBackground = true)
@Composable
fun PreviewStandardTopAppBar() = AeonTopBarInteractivePreviewWrapper {
    AeonTopAppBar(
        left = AeonTopBarSection(
            content = {
                AeonTopBarNavTitlePill(
                    title = "Wallet",
                    subtitle = "Main Balance",
                    onMenuClick = {}
                )
            }
        ),
        right = AeonTopBarSection(
            content = {
                AeonTopBarActionPill {
                    AppBarIconButton(icon = Icons.Rounded.Search, onClick = {})
                }
            }
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "Search Center", group = "Aeon Top Bar", showBackground = true)
@Composable
fun PreviewSearchCenterTopAppBar() = AeonTopBarInteractivePreviewWrapper {
    AeonTopAppBar(
        left = AeonTopBarSection(
            content = { AeonTopBarNavPill(onBackClick = {}) }
        ),
        center = AeonTopBarSection(
            content = {
                AeonTopBarSearchPill(
                    query = "New York",
                    onQueryChange = {},
                    placeholder = "Search location..."
                )
            }
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "Custom Modular", group = "Aeon Top Bar", showBackground = true)
@Composable
fun PreviewCustomModularTopAppBar() = AeonTopBarInteractivePreviewWrapper {
    AeonTopAppBar(
        left = AeonTopBarSection(
            content = {
                AeonTopBarPill {
                    AppBarIconButton(icon = Icons.Rounded.Menu, onClick = {})
                    Text("MENU", style = AppTextStyles.caption.copy(fontWeight = FontWeight.Black))
                }
            }
        ),
        center = AeonTopBarSection(
            content = {
                AeonTopBarPill {
                    Text("STATUS: ACTIVE", style = AppTextStyles.caption.copy(fontWeight = FontWeight.Bold))
                }
            }
        ),
        right = AeonTopBarSection(
            content = {
                AeonTopBarActionPill {
                    AppBarIconButton(icon = Icons.Rounded.Search, onClick = {})
                }
            }
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "Back Title", group = "Aeon Top Bar", showBackground = true)
@Composable
fun PreviewBackTitleTopAppBar() = AeonTopBarInteractivePreviewWrapper {
    AeonTopAppBar(
        left = AeonTopBarSection(
            content = {
                AeonTopBarNavTitlePill(
                    title = "Settings",
                    onBackClick = {}
                )
            }
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "Search Actions", group = "Aeon Top Bar", showBackground = true)
@Composable
fun PreviewSearchActionsTopAppBar() = AeonTopBarInteractivePreviewWrapper {
    AeonTopAppBar(
        left = AeonTopBarSection(
            content = { AeonTopBarNavPill(onBackClick = {}) }
        ),
        center = AeonTopBarSection(
            content = {
                AeonTopBarSearchPill(
                    query = "",
                    onQueryChange = {},
                    placeholder = "Search..."
                )
            }
        ),
        right = AeonTopBarSection(
            content = {
                AeonTopBarActionPill {
                    AppBarIconButton(icon = Icons.Rounded.Search, onClick = {})
                }
            }
        )
    )
}

@Composable
fun AeonTopAppBarPreview(
    theme: String? = null,
    glassEnabled: Boolean? = null,
    content: @Composable () -> Unit
) {
    val config = LocalConfiguration.current
    val isDark = config.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

    val themeName = theme ?: if (isDark) "Dark" else "Light"
    val finalGlassEnabled = glassEnabled ?: true

    OssWalletTheme(theme = themeName) {
        CompositionLocalProvider(
            LocalGlassmorphismEnabled provides finalGlassEnabled,
            LocalLayoutDirection provides LayoutDirection.Ltr
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(AppColors.screenBackground)
            ) {
                // We use AeonHazeProvider to create the necessary HazeState infrastructure.
                AeonHazeProvider(backgroundColor = AppColors.screenBackground) { hazeLayers ->
                    // Capture EVERYTHING behind the Top Bar as a haze source.
                    // Only apply if glass is enabled to prevent unnecessary snapshot state tracking
                    // that can cause render issues in certain Preview configurations.
                    // We only need the first few layers for Top Bar previews.
                    val sourceModifier = if (finalGlassEnabled) {
                        hazeLayers.states.take(3).fold(Modifier as Modifier) { mod, state ->
                            mod.hazeSource(state)
                        }
                    } else Modifier

                    Row(modifier = Modifier.fillMaxSize().then(sourceModifier)) {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(
                            Brush.verticalGradient(listOf(Color(0xFF673AB7).copy(alpha = 0.35f), Color.Transparent))
                        ))
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(
                            Brush.verticalGradient(listOf(Color(0xFF00BCD4).copy(alpha = 0.35f), Color.Transparent))
                        ))
                    }
                    
                    Box(modifier = Modifier.fillMaxSize()) {
                        content()
                    }
                }
            }
        }
    }
}
