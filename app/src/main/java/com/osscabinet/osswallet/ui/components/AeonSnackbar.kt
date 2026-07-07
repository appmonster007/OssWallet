package com.osscabinet.ossWallet.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.ui.theme.AeonSize
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme

/**
 * Defines semantic types for AeonSnackbar to drive distinct visual styles.
 */
enum class AeonSnackbarType {
    DEFAULT,
    SUCCESS,
    ERROR,
    INFO
}

/**
 * Extension for SnackbarVisuals to carry Aeon-specific metadata.
 */
class AeonSnackbarVisuals(
    override val message: String,
    override val actionLabel: String? = null,
    override val withDismissAction: Boolean = false,
    override val duration: SnackbarDuration = SnackbarDuration.Short,
    val type: AeonSnackbarType = AeonSnackbarType.DEFAULT
) : SnackbarVisuals

/**
 * A standardized SnackbarHost for the OssWallet app that ensures consistent
 * floating placement above navigation elements and FABs.
 */
@Composable
fun AeonSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    bottomOffset: Dp = 0.dp,
    showAboveBottomBar: Boolean = true
) {
    val navigationBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    
    // Standard OssWallet bottom bar / FAB height is 56dp
    val baseBottomPadding = if (showAboveBottomBar) {
        56.dp + AeonSize.D16.value + 8.dp
    } else {
        AeonSize.D16.value
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = baseBottomPadding + navigationBarHeight + bottomOffset),
        contentAlignment = Alignment.BottomCenter
    ) {
        SnackbarHost(
            hostState = hostState,
            snackbar = { data ->
                AeonSnackbar(data)
            }
        )
    }
}

/**
 * Custom-styled Snackbar for OssWallet, applying consistent M3 styling 
 * but hooked into AeonTheme dimensions and semantic types.
 */
@Composable
fun AeonSnackbar(
    snackbarData: SnackbarData,
    modifier: Modifier = Modifier,
) {
    val visuals = snackbarData.visuals
    val aeonVisuals = visuals as? AeonSnackbarVisuals
    val type = aeonVisuals?.type ?: AeonSnackbarType.DEFAULT

    val containerColor = when (type) {
        AeonSnackbarType.DEFAULT -> MaterialTheme.colorScheme.inverseSurface
        AeonSnackbarType.SUCCESS -> AppColors.positive.copy(alpha = 0.95f)
        AeonSnackbarType.ERROR -> AppColors.error.copy(alpha = 0.95f)
        AeonSnackbarType.INFO -> MaterialTheme.colorScheme.secondaryContainer
    }

    val contentColor = when (type) {
        AeonSnackbarType.DEFAULT -> MaterialTheme.colorScheme.inverseOnSurface
        AeonSnackbarType.SUCCESS -> Color.White
        AeonSnackbarType.ERROR -> MaterialTheme.colorScheme.onError
        AeonSnackbarType.INFO -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    val actionColor = when (type) {
        AeonSnackbarType.DEFAULT -> MaterialTheme.colorScheme.inversePrimary
        AeonSnackbarType.SUCCESS -> Color.White
        AeonSnackbarType.ERROR -> Color.White
        AeonSnackbarType.INFO -> MaterialTheme.colorScheme.primary
    }

    val icon: ImageVector? = when (type) {
        AeonSnackbarType.SUCCESS -> Icons.Rounded.CheckCircle
        AeonSnackbarType.ERROR -> Icons.Rounded.Error
        AeonSnackbarType.INFO -> Icons.Rounded.Info
        else -> null
    }

    Snackbar(
        modifier = modifier.padding(AeonSize.D12.value),
        actionContentColor = actionColor,
        containerColor = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(AeonTheme.dimensions.cardCornerRadius),
        action = snackbarData.visuals.actionLabel?.let { actionLabel ->
            {
                TextButton(
                    onClick = { snackbarData.performAction() },
                    contentPadding = PaddingValues(horizontal = AeonSize.D8.value)
                ) {
                    Text(
                        text = actionLabel,
                        color = actionColor,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = AeonSize.D4.value)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(AeonSize.D20.value),
                    tint = contentColor
                )
                Spacer(modifier = Modifier.width(AeonSize.D12.value))
            }
            Text(
                text = visuals.message,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/**
 * Helper to show standardized snackbars with semantic types.
 */
suspend fun SnackbarHostState.showAeonSnackbar(
    message: String,
    actionLabel: String? = null,
    duration: SnackbarDuration = SnackbarDuration.Short,
    type: AeonSnackbarType = AeonSnackbarType.DEFAULT
): SnackbarResult {
    return showSnackbar(
        AeonSnackbarVisuals(
            message = message,
            actionLabel = actionLabel,
            duration = duration,
            type = type
        )
    )
}

@Preview(showBackground = true)
@Composable
fun AeonSnackbarPreview() {
    OssWalletTheme {
        Column(
            modifier = Modifier.padding(AeonSize.D16.value),
            verticalArrangement = Arrangement.spacedBy(AeonSize.D16.value)
        ) {
            Text("Default / Actionable", style = MaterialTheme.typography.titleSmall)
            AeonSnackbar(
                snackbarData = MockSnackbarData(
                    message = "Transaction deleted",
                    actionLabel = "Undo",
                    type = AeonSnackbarType.DEFAULT
                )
            )

            Text("Success", style = MaterialTheme.typography.titleSmall)
            AeonSnackbar(
                snackbarData = MockSnackbarData(
                    message = "Transaction saved successfully",
                    type = AeonSnackbarType.SUCCESS
                )
            )

            Text("Error", style = MaterialTheme.typography.titleSmall)
            AeonSnackbar(
                snackbarData = MockSnackbarData(
                    message = "Failed to sync with server",
                    actionLabel = "Retry",
                    type = AeonSnackbarType.ERROR
                )
            )

            Text("Info", style = MaterialTheme.typography.titleSmall)
            AeonSnackbar(
                snackbarData = MockSnackbarData(
                    message = "New feature available in settings",
                    type = AeonSnackbarType.INFO
                )
            )
        }
    }
}

private class MockSnackbarData(
    message: String,
    actionLabel: String? = null,
    type: AeonSnackbarType = AeonSnackbarType.DEFAULT
) : SnackbarData {
    override val visuals: SnackbarVisuals = AeonSnackbarVisuals(
        message = message,
        actionLabel = actionLabel,
        type = type
    )
    override fun dismiss() {}
    override fun performAction() {}
}
