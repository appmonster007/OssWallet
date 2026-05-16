package com.osscabinet.ossWallet.ui.components.cards

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.card.CardFilterRules
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.ui.components.transactions.countActiveFilters
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles

/**
 * A reusable card component following the "Architect Slate" design specification.
 * 
 * Rules:
 * Corner Radius: 24dp (AeonTheme.dimensions.cardCornerRadius)
 * Background: Surface Container Low
 * Padding: 16dp internal (AeonTheme.dimensions.cardPadding)
 * Elevation: Flat (0dp)
 * Interactive: Scale-98 on press
 */
@Composable
fun AeonExpressiveCard(
    title: String,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconColor: Color = AppColors.primary,
    filter: TransactionFilter? = null,
    baseFilter: TransactionFilter = TransactionFilter(),
    filterRules: CardFilterRules? = null,
    headerTrailing: @Composable (RowScope.(Int) -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    isWarning: Boolean = false,
    accentColor: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.98f else 1f, label = "cardScale")
    
    val activeFilterCount = filter?.let { countActiveFilters(it, baseFilter, filterRules) } ?: 0

    Card(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (onClick != null) Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    onClick = onClick
                ) else Modifier
            ),
        shape = RoundedCornerShape(AeonTheme.dimensions.cardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = AppColors.cardSurface
        ),
        border = BorderStroke(
            width = if (accentColor != null) 1.dp else 0.5.dp,
            color = when {
                isWarning -> AppColors.error.copy(alpha = 0.3f)
                accentColor != null -> accentColor.copy(alpha = 0.4f)
                else -> AppColors.divider.copy(alpha = 0.15f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AeonTheme.dimensions.cardPadding)
        ) {
            // Header Composition (Icon + Title + Subtitle)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(headerModifier),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm)
            ) {
                if (icon != null) {
                    Surface(
                        modifier = Modifier.size(AeonTheme.dimensions.iconSizeLg),
                        shape = CircleShape,
                        color = iconColor.copy(alpha = 0.15f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(AeonTheme.dimensions.iconSizeSm),
                                tint = iconColor
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = AppTextStyles.screenSubTitle.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            letterSpacing = 0.sp
                        ),
                        color = AppColors.contentPrimary
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = AppTextStyles.drawerItemLabel.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp
                            ),
                            color = AppColors.contentSecondary
                        )
                    }
                }

                if (headerTrailing != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingXs)
                    ) {
                        headerTrailing(activeFilterCount)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(AeonTheme.dimensions.spacingSm)) // Vertical Rhythm: 8dp gap
            
            content()
        }
    }
}

/**
 * Standard filter button for expressive cards that indicates active filters.
 */
@Composable
fun AeonCardFilterButton(
    onClick: () -> Unit,
    activeFilterCount: Int,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Rounded.FilterList,
    activeIcon: ImageVector = Icons.Rounded.FilterAlt
) {
    val isActive = activeFilterCount > 0
    
    Surface(
        onClick = onClick,
        modifier = modifier.size(28.dp),
        shape = CircleShape,
        color = if (isActive) AppColors.primary.copy(alpha = 0.15f) 
                else AppColors.transparent,
        border = if (isActive) BorderStroke(1.dp, AppColors.primary.copy(alpha = 0.5f))
                 else null
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (isActive) activeIcon else icon,
                contentDescription = "Filter",
                tint = if (isActive) AppColors.primary 
                       else AppColors.contentSecondary.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}


