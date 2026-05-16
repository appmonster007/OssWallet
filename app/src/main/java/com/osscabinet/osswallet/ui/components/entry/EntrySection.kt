package com.osscabinet.ossWallet.ui.components.entry

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.rounded.*
import com.osscabinet.ossWallet.ui.theme.AppColors

/**
 * Standard container for metadata/details in entry screens.
 * Following the "Architect Slate" design specification.
 */
@Composable
fun EntrySection(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AeonTheme.dimensions.cardCornerRadius),
        color = AppColors.sheetSurface,
        border = BorderStroke(0.5.dp, AppColors.divider.copy(alpha = 0.15f)),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(AeonTheme.dimensions.cardPadding),
            verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
        ) {
            // Section Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
            ) {
                Surface(
                    shape = CircleShape,
                    color = AppColors.primary.copy(alpha = 0.1f),
                    modifier = Modifier.size(AeonTheme.dimensions.iconSizeLg)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            icon,
                            contentDescription = null,
                            modifier = Modifier.size(AeonTheme.dimensions.iconSizeSm),
                            tint = AppColors.primary
                        )
                    }
                }
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        fontSize = 10.sp
                    ),
                    color = AppColors.contentSecondary.copy(alpha = 0.6f)
                )
            }

            content()
        }
    }
}

@Preview(showBackground = true, name = "Light Mode")
@Composable
fun EntrySectionLightPreview() {
    OssWalletTheme(theme = "Light") {
        Surface(
            color = AppColors.screenBackground,
            modifier = Modifier.padding(AeonTheme.dimensions.screenPadding)
        ) {
            EntrySection(
                title = "Date Information",
                icon = Icons.Default.CalendarToday
            ) {
                Text(
                    text = "Sample content inside the section",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "More content can be added here",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.contentSecondary
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Amoled Mode")
@Composable
fun EntrySectionAmoledPreview() {
    OssWalletTheme(theme = "Amoled") {
        Surface(
            color = AppColors.screenBackground,
            modifier = Modifier.padding(AeonTheme.dimensions.screenPadding)
        ) {
            EntrySection(
                title = "Metadata Details",
                icon = Icons.Rounded.Info
            ) {
                Text(
                    text = "Premium Amoled rendering with Carbon Slate container.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(AeonTheme.dimensions.spacingSm))
                LinearProgressIndicator(
                    progress = { 0.6f },
                    modifier = Modifier.fillMaxWidth(),
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Transaction Entry Details")
@Composable
fun TransactionEntrySectionPreview() {
    OssWalletTheme(theme = "Amoled") {
        Surface(
            color = AppColors.screenBackground,
            modifier = Modifier.padding(AeonTheme.dimensions.screenPadding)
        ) {
            AeonDetailSection(
                note = "Dinner with friends at the new Italian place",
                onNoteChange = {},
                payee = "Luigi's Italian",
                onPayeeChange = {},
                selectedTags = listOf(
                    com.osscabinet.ossWallet.domain.model.Tag(name = "Food", colorHex = 0xFFEF5350.toInt()),
                    com.osscabinet.ossWallet.domain.model.Tag(name = "Social", colorHex = 0xFFAB47BC.toInt())
                ),
                additionalContent = {
                    Column(verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingLg)) {
                        // Linked Transactions mock
                        EntryLinkedTransactionsSection(
                            linkedTransactions = emptyList(),
                            accounts = emptyList(),
                            categories = emptyList(),
                            is24Hour = true,
                            onShowSheet = {},
                            onRemoveLink = {}
                        )

                        EntryPaymentMethodStatusRow(
                            paymentMethod = "Cash",
                            onPaymentMethodClick = {},
                            statusLabel = "Cleared",
                            onStatusClick = {},
                            paymentMethodIcon = Icons.Rounded.Payments,
                            statusIcon = Icons.Rounded.CheckCircle,
                            statusIconTint = AppColors.primary,
                            statusContainerColor = AppColors.primary.copy(alpha = 0.1f)
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = AeonTheme.dimensions.spacingXs),
                            thickness = (AeonTheme.dimensions.spacingXs / 8),
                            color = AppColors.divider.copy(alpha = 0.2f)
                        )

                        EntryWarrantySection(
                            enabled = true,
                            onToggle = {},
                            expirationMillis = System.currentTimeMillis() + 31536000000L,
                            onDateClick = {}
                        )

                        EntryLocationRow(
                            coords = null,
                            onShowLocationSheet = {},
                            label = "Transaction Location"
                        )
                    }
                }
            )
        }
    }
}
