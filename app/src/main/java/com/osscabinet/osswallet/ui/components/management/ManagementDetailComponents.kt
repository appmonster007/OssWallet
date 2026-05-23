package com.osscabinet.ossWallet.ui.components.management

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme

/**
 * Shared chip for displaying scope/context items (Accounts, Categories, Tags)
 */
@Composable
fun ScopeChip(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(0.5.dp, color.copy(alpha = 0.2f)),
        modifier = modifier
    ) {
        Text(
            text = text,
            style = AppTextStyles.cardDelta,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
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

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                color = AppColors.primary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = AppColors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Text(
                label,
                style = AppTextStyles.transactionSwipeAction.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = AppColors.primary.copy(alpha = 0.6f)
            )
        }

        if (isEmpty) {
            Text(
                allText,
                style = AppTextStyles.transactionAmount,
                color = AppColors.contentPrimary,
                modifier = Modifier.padding(start = 56.dp)
            )
        } else {
            FlowRow(
                modifier = Modifier.padding(start = 56.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val displayCount = if (isExpanded) items.size else 5
                items.take(displayCount).forEach { it() }

                if (items.size > 5 && !isExpanded) {
                    Surface(
                        onClick = { isExpanded = true },
                        color = AppColors.primary.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(
                            0.5.dp,
                            AppColors.primary.copy(alpha = 0.1f)
                        )
                    ) {
                        Text(
                            text = "+${items.size - 5} more",
                            style = AppTextStyles.cardDelta,
                            color = AppColors.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ScopeChipPreview() {
    OssWalletTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            ScopeChip(text = "Groceries", color = Color.Magenta)
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
                items = listOf(
                    { ScopeChip("Food", Color.Red) },
                    { ScopeChip("Drinks", Color.Blue) },
                    { ScopeChip("Snacks", Color.Green) },
                    { ScopeChip("Dinner", Color.Yellow) },
                    { ScopeChip("Breakfast", Color.Cyan) },
                    { ScopeChip("Lunch", Color.Magenta) }
                )
            )
        }
    }
}
