package com.osscabinet.ossWallet.ui.components

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val visible: Boolean = true
)

/**
 * Constants and Style Defaults for the Aeon TopAppBar.
 */
object AeonTopBarDefaults {
    val Height = AeonSize.D64.value
    val PillHeight = AeonSize.D48.value
    val HorizontalPadding = AeonSize.D12.value
    val ContentPadding = PaddingValues(AeonSize.D4.value)
    
    val TitleStyle: androidx.compose.ui.text.TextStyle
        @Composable get() = AppTextStyles.screenTitle.copy(
            fontSize = 16.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.sp
        )
    
    val SubtitleStyle: androidx.compose.ui.text.TextStyle
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
 * Standard high-level implementation of AeonTopAppBar.
 * Uses conventional Title/Nav/Action parameters and automatically organizes them
 * into the standardized Left and Right sections.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AeonTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBackClick: (() -> Unit)? = null,
    onMenuClick: (() -> Unit)? = null,
    isTopLevel: Boolean = onMenuClick != null,
    actions: @Composable (RowScope.() -> Unit)? = null,
    tab: AeonAppBarTab? = null,
    titleContent: @Composable (() -> Unit)? = null,
    subtitleContent: @Composable (() -> Unit)? = null,
    windowInsets: WindowInsets = AeonTopBarDefaults.windowInsets(),
) {
    val hasNavigation = (isTopLevel && onMenuClick != null) || onBackClick != null
    val hasTitle = title.isNotEmpty() || titleContent != null
    val displaySubtitle = subtitle ?: tab?.label

    AeonTopAppBar(
        modifier = modifier,
        left = if (hasNavigation || hasTitle) {
            AeonTopBarSection(
                content = {
                    AeonTopBarNavTitlePill(
                        title = title,
                        subtitle = displaySubtitle,
                        onBackClick = onBackClick,
                        onMenuClick = onMenuClick,
                        isTopLevel = isTopLevel,
                        titleContent = titleContent,
                        subtitleContent = subtitleContent
                    )
                }
            )
        } else null,
        right = if (actions != null || tab?.actions != null) {
            AeonTopBarSection(
                content = {
                    AeonTopBarActionPill {
                        actions?.invoke(this)
                        tab?.actions?.invoke(this)
                    }
                }
            )
        } else null,
        windowInsets = windowInsets
    )
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
@Composable
fun AeonTopBarNavTitlePill(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBackClick: (() -> Unit)? = null,
    onMenuClick: (() -> Unit)? = null,
    isTopLevel: Boolean = onMenuClick != null,
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
            titleContent = titleContent,
            subtitleContent = subtitleContent
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
 * Standard Title/Subtitle component.
 */
@Composable
fun AeonTopBarTitle(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    titleContent: @Composable (() -> Unit)? = null,
    subtitleContent: @Composable (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.padding(vertical = AeonSize.D4.value),
        verticalArrangement = Arrangement.Center
    ) {
        if (titleContent != null) {
            titleContent()
        } else if (title.isNotEmpty()) {
            Text(
                title,
                style = AeonTopBarDefaults.TitleStyle,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.onScreenBackground
            )
        }
        if (subtitleContent != null) {
            subtitleContent()
        } else if (subtitle != null) {
            Text(
                subtitle,
                style = AeonTopBarDefaults.SubtitleStyle,
                color = AppColors.contentSecondary.copy(alpha = 0.8f)
            )
        }
    }
}

data class AeonAppBarTab(
    val label: String,
    val actions: @Composable (RowScope.() -> Unit)? = null
)

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
 * Matrix of theme and glass variations for Aeon previews.
 */
data class AeonPreviewState(
    val theme: String,
    val glass: Boolean,
    val name: String
) {
    override fun toString(): String = name
}

class AeonPreviewProvider : PreviewParameterProvider<AeonPreviewState> {
    override val values = sequenceOf(
        AeonPreviewState("Light", true, "Light Glass"),
        AeonPreviewState("Light", false, "Light Solid"),
        AeonPreviewState("Dark", true, "Dark Glass"),
        AeonPreviewState("Dark", false, "Dark Solid"),
        AeonPreviewState("Amoled", true, "Amoled Glass"),
        AeonPreviewState("Amoled", false, "Amoled Solid")
    )
}

@Preview(group = "Aeon Top Bar", showBackground = true)
@Composable
fun PreviewStandardTopAppBar(
    @PreviewParameter(AeonPreviewProvider::class) state: AeonPreviewState
) = AeonTopAppBarPreview(theme = state.theme, glassEnabled = state.glass) {
    AeonTopAppBar(
        title = "Wallet",
        subtitle = "Main Balance",
        onMenuClick = {},
        actions = {
            AppBarIconButton(icon = Icons.Rounded.Search, onClick = {})
        }
    )
}

@Preview(group = "Aeon Top Bar", showBackground = true)
@Composable
fun PreviewSearchCenterTopAppBar(
    @PreviewParameter(AeonPreviewProvider::class) state: AeonPreviewState
) = AeonTopAppBarPreview(theme = state.theme, glassEnabled = state.glass) {
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

@Preview(group = "Aeon Top Bar", showBackground = true)
@Composable
fun PreviewCustomModularTopAppBar(
    @PreviewParameter(AeonPreviewProvider::class) state: AeonPreviewState
) = AeonTopAppBarPreview(theme = state.theme, glassEnabled = state.glass) {
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

@Preview(group = "Aeon Top Bar", showBackground = true)
@Composable
fun PreviewBackTitleTopAppBar(
    @PreviewParameter(AeonPreviewProvider::class) state: AeonPreviewState
) = AeonTopAppBarPreview(theme = state.theme, glassEnabled = state.glass) {
    AeonTopAppBar(
        title = "Settings",
        onBackClick = {}
    )
}

@Preview(group = "Aeon Top Bar", showBackground = true)
@Composable
fun PreviewSearchActionsTopAppBar(
    @PreviewParameter(AeonPreviewProvider::class) state: AeonPreviewState
) = AeonTopAppBarPreview(theme = state.theme, glassEnabled = state.glass) {
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
