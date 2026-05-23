package com.osscabinet.ossWallet.ui.components.management

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme

/**
 * Shared section header for management list screens (Budgets, Loans, etc.)
 */
@Composable
fun ManagementSectionHeader(
    title: String,
    sectionColor: Color,
    modifier: Modifier = Modifier,
    count: Int? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.padding(vertical = 8.dp, horizontal = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(sectionColor)
        )
        Text(
            text = title.uppercase(),
            style = AppTextStyles.transactionSwipeAction.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 10.sp,
                letterSpacing = 1.5.sp
            ),
            color = sectionColor,
            modifier = Modifier.weight(1f)
        )
        if (count != null) {
            Surface(
                shape = CircleShape,
                color = sectionColor.copy(alpha = 0.12f)
            ) {
                Text(
                    text = count.toString(),
                    style = AppTextStyles.transactionSwipeAction.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    ),
                    color = sectionColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ManagementSectionHeaderPreview() {
    OssWalletTheme {
        Surface {
            ManagementSectionHeader(
                title = "Active Budgets",
                sectionColor = Color(0xFF004D5F),
                count = 3
            )
        }
    }
}
