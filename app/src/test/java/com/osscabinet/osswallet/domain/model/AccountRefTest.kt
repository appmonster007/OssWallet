package com.osscabinet.ossWallet.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountRefTest {

    @Test
    fun `fromStored maps null to Unselected`() {
        assertEquals(AccountRef.Unselected, AccountRef.fromStored(null))
    }

    @Test
    fun `fromStored maps empty string to Outside`() {
        assertEquals(AccountRef.Outside, AccountRef.fromStored(""))
    }

    @Test
    fun `fromStored maps blank string to Outside`() {
        assertEquals(AccountRef.Outside, AccountRef.fromStored("   "))
    }

    @Test
    fun `fromStored maps non-blank id to Wallet`() {
        assertEquals(AccountRef.Wallet("acc1"), AccountRef.fromStored("acc1"))
    }

    @Test
    fun `fromTransferSide maps null to Outside`() {
        assertEquals(AccountRef.Outside, AccountRef.fromTransferSide(null))
    }

    @Test
    fun `fromTransferSide maps blank to Outside`() {
        assertEquals(AccountRef.Outside, AccountRef.fromTransferSide(""))
    }

    @Test
    fun `fromTransferSide maps real id to Wallet`() {
        assertEquals(AccountRef.Wallet("acc1"), AccountRef.fromTransferSide("acc1"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Wallet rejects blank id`() {
        AccountRef.Wallet("")
    }

    @Test
    fun `toStored round-trips fromStored`() {
        assertNull(AccountRef.fromStored(null).toStored())
        assertEquals("", AccountRef.fromStored("").toStored())
        assertEquals("acc1", AccountRef.fromStored("acc1").toStored())
    }

    @Test
    fun `toDbTransferSide encodes Outside as null`() {
        assertNull(AccountRef.Outside.toDbTransferSide())
        assertNull(AccountRef.Unselected.toDbTransferSide())
        assertEquals("acc1", AccountRef.Wallet("acc1").toDbTransferSide())
    }

    @Test
    fun `resolve returns OUTSIDE_WALLET for Outside`() {
        val accounts = listOf(
            Account("acc1", "Cash", "USD", AccountStructureType.OTHER, "#000", "ic")
        )
        assertEquals(Account.OUTSIDE_WALLET, AccountRef.Outside.resolve(accounts))
        assertNull(AccountRef.Unselected.resolve(accounts))
        assertEquals("acc1", AccountRef.Wallet("acc1").resolve(accounts)?.id)
        assertNull(AccountRef.Wallet("missing").resolve(accounts))
    }

    @Test
    fun `isPicked is false only for Unselected`() {
        assertFalse(AccountRef.Unselected.isPicked)
        assertTrue(AccountRef.Outside.isPicked)
        assertTrue(AccountRef.Wallet("acc1").isPicked)
    }
}
