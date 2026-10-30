package com.osscabinet.ossWallet.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.BuildConfig
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme

/**
 * Periodic nudge (see [com.osscabinet.ossWallet.domain.service.BuyMeACoffeePromptScheduler])
 * pointing users at the project's Buy Me a Coffee page and its membership tiers. Dismissing
 * it only snoozes it until the next scheduled window — there is no permanent opt-out. The
 * same tiers/CTAs are also reachable any time from Settings → Support Us.
 */
@Composable
fun BuyMeACoffeeSupportDialog(
    show: Boolean,
    onDismiss: () -> Unit
) {
    if (!show) return
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { BuyMeACoffeeCupIcon() },
        title = { Text("Enjoying OssWallet?", style = AppTextStyles.heroAmount) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "OssWallet is free and open source. If it's useful to you, " +
                        "consider supporting its development — as a one-off coffee " +
                        "or a monthly membership."
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SUPPORT_TIERS.forEach { tier ->
                        SupportTierChip(
                            tier = tier,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                openExternalUrl(context, "${BuildConfig.BUY_ME_A_COFFEE_URL}/membership")
                            }
                        )
                    }
                }
                Text(
                    "Members get exclusive posts and messages.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                openExternalUrl(context, BuildConfig.BUY_ME_A_COFFEE_URL)
                onDismiss()
            }) {
                Text("Buy me a coffee")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Maybe later")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun BuyMeACoffeeSupportDialogPreview() {
    OssWalletTheme {
        BuyMeACoffeeSupportDialog(show = true, onDismiss = {})
    }
}
