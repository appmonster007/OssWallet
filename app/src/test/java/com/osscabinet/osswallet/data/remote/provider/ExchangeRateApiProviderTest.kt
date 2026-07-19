package com.osscabinet.ossWallet.data.remote.provider

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExchangeRateApiProviderTest {

    @Test
    fun `test all currencies are present`() {
        val currencies = ExchangeRateApiProvider.SupportedCurrencies.entries
        // Verified count is 162
        assertEquals(162, currencies.size)
    }

    @Test
    fun `test volatile currencies have notes`() {
        val volatileCurrencies = ExchangeRateApiProvider.SupportedCurrencies.entries.filter { it.isVolatile }
        
        // Expected volatile codes from the user list
        val expectedCodes = listOf("ARS", "LYD", "SSP", "SYP", "VES", "YER", "ZWL")
        
        assertEquals(expectedCodes.size, volatileCurrencies.size)
        
        volatileCurrencies.forEach { 
            assertTrue(expectedCodes.contains(it.code))
            assertTrue(it.note?.contains("volatility") == true)
        }
    }

    @Test
    fun `test specific currency metadata`() {
        val usd = ExchangeRateApiProvider.SupportedCurrencies.USD.toMetadata()
        assertEquals("USD", usd.code)
        assertEquals("United States Dollar", usd.name)
        assertEquals("United States", usd.country)
        
        val ars = ExchangeRateApiProvider.SupportedCurrencies.ARS.toMetadata()
        assertTrue(ars.isVolatile)
        assertTrue(ars.note != null)
    }
}
