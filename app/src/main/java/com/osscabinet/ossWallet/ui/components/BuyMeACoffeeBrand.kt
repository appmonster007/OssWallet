package com.osscabinet.ossWallet.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.R
import com.osscabinet.ossWallet.ui.theme.BuyMeACoffeeInk
import com.osscabinet.ossWallet.ui.theme.BuyMeACoffeeYellow

/**
 * Shared Buy Me a Coffee brand elements — reused by [BuyMeACoffeeSupportDialog] (the periodic
 * nudge) and the Settings "Support Us" page, so both present the same tiers and iconography.
 */
internal data class SupportTier(val label: String, val priceLine: String)

internal val SUPPORT_TIERS = listOf(
    SupportTier("Tier 1", "$2/mo"),
    SupportTier("Tier 2", "$5/mo"),
    SupportTier("Tier 3", "$10/mo")
)

internal fun openExternalUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: ActivityNotFoundException) {
        // No browser available — nothing to open.
    }
}

@Composable
internal fun BuyMeACoffeeCupIcon(modifier: Modifier = Modifier) {
    val cup: Painter = painterResource(R.drawable.ic_bmc_cup)
    Image(painter = cup, contentDescription = null, modifier = modifier)
}

@Composable
internal fun SupportTierChip(
    tier: SupportTier,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = BuyMeACoffeeYellow
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                tier.label,
                style = MaterialTheme.typography.labelSmall,
                color = BuyMeACoffeeInk
            )
            Text(
                tier.priceLine,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = BuyMeACoffeeInk
            )
        }
    }
}
