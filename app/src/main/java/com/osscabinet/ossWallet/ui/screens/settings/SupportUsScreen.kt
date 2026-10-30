package com.osscabinet.ossWallet.ui.screens.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.BuildConfig
import com.osscabinet.ossWallet.R
import com.osscabinet.ossWallet.ui.components.AeonHazeProvider
import com.osscabinet.ossWallet.ui.components.AeonSettingsSection
import com.osscabinet.ossWallet.ui.components.AeonTopAppBar
import com.osscabinet.ossWallet.ui.components.AeonTopBarNavTitlePill
import com.osscabinet.ossWallet.ui.components.AeonTopBarSection
import com.osscabinet.ossWallet.ui.components.BuyMeACoffeeCupIcon
import com.osscabinet.ossWallet.ui.components.SUPPORT_TIERS
import com.osscabinet.ossWallet.ui.components.SupportTier
import com.osscabinet.ossWallet.ui.components.aeonHazeSource
import com.osscabinet.ossWallet.ui.components.openExternalUrl
import com.osscabinet.ossWallet.ui.theme.AeonSize
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.BuyMeACoffeeInk
import com.osscabinet.ossWallet.ui.theme.BuyMeACoffeeYellow
import com.osscabinet.ossWallet.ui.theme.HazeLayer
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportUsScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val membershipUrl = "${BuildConfig.BUY_ME_A_COFFEE_URL}/membership"

    AeonHazeProvider(backgroundColor = AppColors.screenBackground) { hazeLayers ->
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0)
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .aeonHazeSource(hazeLayers.states[HazeLayer.Overlay])
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .statusBarsPadding()
                            .padding(
                                top = AeonSize.D64.value + AeonTheme.dimensions.screenPadding,
                                bottom = 40.dp
                            )
                            .padding(horizontal = AeonTheme.dimensions.screenPadding),
                        verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacing2xl)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
                        ) {
                            BuyMeACoffeeCupIcon(modifier = Modifier.padding(top = 8.dp))
                            Text(
                                "Support OssWallet",
                                style = AppTextStyles.heroAmount,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                "OssWallet is free, open source, and has no ads or trackers. " +
                                    "If it's useful to you, a coffee or a monthly membership " +
                                    "helps keep development going.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppColors.contentSecondary,
                                textAlign = TextAlign.Center
                            )
                        }

                        AeonSettingsSection(
                            title = "MEMBERSHIP TIERS",
                            accentBar = BuyMeACoffeeYellow
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = AeonTheme.dimensions.cardPadding,
                                        vertical = AeonTheme.dimensions.spacingSm
                                    ),
                                verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm)
                            ) {
                                SUPPORT_TIERS.forEach { tier ->
                                    MembershipTierRow(
                                        tier = tier,
                                        onClick = { openExternalUrl(context, membershipUrl) }
                                    )
                                }
                            }
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm)
                        ) {
                            Button(
                                onClick = { openExternalUrl(context, membershipUrl) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BuyMeACoffeeYellow,
                                    contentColor = BuyMeACoffeeInk
                                )
                            ) {
                                Text("Become a Member")
                            }
                            OutlinedButton(
                                onClick = { openExternalUrl(context, BuildConfig.BUY_ME_A_COFFEE_URL) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Buy me a one-off coffee")
                            }
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm)
                        ) {
                            Text(
                                "SCAN TO SUPPORT FROM ANOTHER DEVICE",
                                style = AppTextStyles.sectionOverline,
                                color = AppColors.contentSecondary.copy(alpha = 0.6f)
                            )
                            Image(
                                painter = painterResource(R.drawable.qr_buy_me_a_coffee),
                                contentDescription = "QR code linking to the OssWallet Buy Me a Coffee page",
                                modifier = Modifier
                                    .size(160.dp)
                                    .border(
                                        0.5.dp,
                                        AppColors.outlineVariant,
                                        RoundedCornerShape(AeonTheme.dimensions.cardCornerRadius)
                                    )
                                    .padding(8.dp)
                            )
                        }
                    }
                }
            }

            AeonTopAppBar(
                left = AeonTopBarSection(
                    content = {
                        AeonTopBarNavTitlePill(
                            title = "Support Us",
                            onBackClick = onBackClick
                        )
                    }
                )
            )
        }
    }
}

@Composable
private fun MembershipTierRow(tier: SupportTier, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = AeonTheme.dimensions.spacingSm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            tier.label,
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.contentPrimary,
            modifier = Modifier.weight(1f)
        )
        Text(
            tier.priceLine,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = AppColors.accent
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SupportUsScreenPreview() {
    OssWalletTheme {
        SupportUsScreen(onBackClick = {})
    }
}
