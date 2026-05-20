package com.osscabinet.ossWallet.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.settings.AccountDetailSettings
import com.osscabinet.ossWallet.domain.model.settings.AccountsViewSettings
import com.osscabinet.ossWallet.domain.model.settings.DashboardSettings
import com.osscabinet.ossWallet.domain.model.settings.TransactionSectionSettings
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.AppColors

@Preview(showBackground = true)
@Composable
fun AeonHeroHeaderPreview() {
    OssWalletTheme {
        Surface(color = AppColors.screenBackground) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AeonHeroHeader(
                    title = "Savings Account",
                    subtitle = "USD 1,250.00",
                    iconName = "AccountBalance",
                    accentColorHex = "#004D5F"
                )
                
                AeonHeroHeader(
                    title = "Credit Card",
                    subtitle = "EUR 0.00",
                    iconName = "CreditCard",
                    accentColorHex = "#B71C1C"
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun VisualIdentitySectionPreview() {
    OssWalletTheme {
        Surface(color = AppColors.screenBackground) {
            Column(modifier = Modifier.padding(16.dp)) {
                VisualIdentitySection(
                    selectedIconName = "AccountBalance",
                    onIconSelected = {},
                    accentColorHex = "#004D5F",
                    onColorSelected = {}
                )
            }
        }
    }
}
