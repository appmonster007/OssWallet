package com.osscabinet.osswallet.domain.service.security

import com.osscabinet.ossWallet.domain.service.security.PinHasher
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinHasherTest {

    @Test
    fun `correct pin verifies`() {
        val salt = PinHasher.newSalt()
        val hash = PinHasher.hash("1234".toCharArray(), salt)
        assertTrue(PinHasher.verify("1234".toCharArray(), salt, hash))
    }

    @Test
    fun `wrong pin is rejected`() {
        val salt = PinHasher.newSalt()
        val hash = PinHasher.hash("1234".toCharArray(), salt)
        assertFalse(PinHasher.verify("1235".toCharArray(), salt, hash))
    }

    @Test
    fun `same pin with different salts hashes differently`() {
        val a = PinHasher.hash("1234".toCharArray(), PinHasher.newSalt())
        val b = PinHasher.hash("1234".toCharArray(), PinHasher.newSalt())
        assertNotEquals(a, b)
    }

    @Test
    fun `hash never contains the pin`() {
        val hash = PinHasher.hash("987654".toCharArray(), PinHasher.newSalt())
        assertFalse(hash.contains("987654"))
    }
}
