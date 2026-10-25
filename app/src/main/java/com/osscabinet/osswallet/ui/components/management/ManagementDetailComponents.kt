package com.osscabinet.ossWallet.ui.components.management

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme

/**
 * Shared chip for displaying scope/context items (Accounts, Categories, Tags)
 * Follows Aeon design philosophy: Pill shape, subtle background, colored dot indicator or circular icon.
 *
 * Optionally tappable ([onClick]) and/or carrying a trailing count badge ([badgeCount], shown
 * only when > 1) — used by category drill-down chips as well as plain static ones. [onClick]
 * is applied via [Modifier.clickable] rather than `Surface(onClick = ...)` so a tappable chip
 * stays exactly the same size as a static one (no Material3 minimum-touch-target inflation),
 * keeping them aligned when they sit side by side in the same row.
 */
@Composable
fun ScopeChip(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    badgeCount: Int? = null,
    onClick: (() -> Unit)? = null
) {
    Surface(
        color = AppColors.cardSurfaceProminent.copy(alpha = 0.6f),
        shape = RoundedCornerShape(12.dp),
        modifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier
    ) {
        Row(
            modifier = Modifier.padding(start = 6.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(12.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(color, CircleShape)
                    )
                }
            }
            Text(
                text = text,
                style = AppTextStyles.caption.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 0.2.sp
                ),
                color = AppColors.contentPrimary.copy(alpha = 0.8f)
            )
            if (badgeCount != null && badgeCount > 1) {
                Surface(
                    color = color.copy(alpha = 0.18f),
                    shape = CircleShape
                ) {
                    Text(
                        text = badgeCount.toString(),
                        style = AppTextStyles.caption.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp
                        ),
                        color = color,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}

/**
 * Shared section for displaying a list of scope items with an icon and label.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScopeSection(
    label: String,
    icon: ImageVector,
    allText: String,
    isEmpty: Boolean,
    items: List<@Composable () -> Unit>,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(AppColors.primary.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = AppColors.primary,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                label,
                style = AppTextStyles.caption.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 10.sp,
                    letterSpacing = 1.2.sp
                ),
                color = AppColors.contentSecondary.copy(alpha = 0.5f)
            )
        }

        if (isEmpty) {
            Text(
                allText,
                style = AppTextStyles.managementItemTitle.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                ),
                color = AppColors.contentSecondary.copy(alpha = 0.35f),
                modifier = Modifier.padding(start = 40.dp)
            )
        } else {
            FlowRow(
                modifier = Modifier.padding(start = 40.dp),
                horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm),
                verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm)
            ) {
                val displayLimit = 12
                val displayCount = if (isExpanded) items.size else displayLimit
                items.take(displayCount).forEach { it() }

                if (items.size > displayLimit && !isExpanded) {
                    Surface(
                        onClick = { isExpanded = true },
                        color = AppColors.primary.copy(alpha = 0.05f),
                        shape = AeonTheme.dimensions.chipCornerRadius.let { RoundedCornerShape(it) }
                    ) {
                        Text(
                            text = "+${items.size - displayLimit} more",
                            style = AppTextStyles.caption.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            color = AppColors.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Section header for management screens
 */
@Composable
fun ManagementSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    sectionColor: Color = AppColors.primary
) {
    Row(
        modifier = modifier.padding(vertical = AeonTheme.dimensions.spacingSm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(sectionColor, CircleShape)
        )
        Text(
            text = title,
            style = AppTextStyles.caption.copy(
                fontWeight = FontWeight.Black,
                fontSize = 11.sp,
                letterSpacing = 1.5.sp
            ),
            color = AppColors.contentSecondary.copy(alpha = 0.7f)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ScopeChipPreview() {
    OssWalletTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ScopeChip(text = "Groceries", color = Color.Magenta)
            ScopeChip(text = "Savings Account", color = Color.Blue)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ScopeSectionPreview() {
    OssWalletTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            ScopeSection(
                label = "CATEGORIES",
                icon = Icons.Rounded.Category,
                allText = "All Categories",
                isEmpty = false,
                items = List(15) { index ->
                    {
                        ScopeChip(
                            "Category $index",
                            Color(0xFF000000.toInt() or (index * 0x123456))
                        )
                    }
                }
            )
        }
    }
}
