package com.osscabinet.ossWallet.ui.screens.previews

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.osscabinet.ossWallet.domain.model.currency.CurrencyRate
import com.osscabinet.ossWallet.ui.screens.settings.ConverterTab
import com.osscabinet.ossWallet.ui.screens.settings.CurrencyUiState
import com.osscabinet.ossWallet.ui.screens.settings.RatesTab
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme

@Preview(showBackground = true, name = "Rates Tab")
@Composable
fun RatesTabPreview() {
    val mockRates = listOf(
        CurrencyRate("USD", "USD", 1.0, 0, 0),
        CurrencyRate("EUR", "USD", 0.92, 0, 0),
        CurrencyRate("GBP", "USD", 0.79, 0, 0),
        CurrencyRate("JPY", "USD", 150.12, 0, 0),
        CurrencyRate("INR", "USD", 83.0, 0, 0),
        CurrencyRate("AUD", "USD", 1.52, 0, 0),
        CurrencyRate("CAD", "USD", 1.35, 0, 0)
    )
    val state = CurrencyUiState(
        rates = mockRates,
        baseCurrency = "USD",
        lastUpdated = System.currentTimeMillis()
    )
    OssWalletTheme {
        Surface {
            RatesTab(uiState = state)
        }
    }
}

@Preview(showBackground = true, name = "Converter Tab")
@Composable
fun ConverterTabPreview() {
    val mockRates = listOf(
        CurrencyRate("USD", "USD", 1.0, 0, 0),
        CurrencyRate("EUR", "USD", 0.92, 0, 0),
        CurrencyRate("GBP", "USD", 0.79, 0, 0)
    )
    val state = CurrencyUiState(
        rates = mockRates,
        baseCurrency = "USD"
    )
    OssWalletTheme {
        Surface {
            ConverterTab(
                uiState = state,
                onConvert = { amount, from, to, callback ->
                    // Mock conversion: very simple for preview
                    callback(amount * 0.92)
                }
            )
        }
    }
}
