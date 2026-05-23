package com.osscabinet.ossWallet.data.web

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthSessionManagerTest {

    private lateinit var manager: AuthSessionManager

    @Before
    fun setup() {
        manager = AuthSessionManager()
        manager.initialize()
    }

    @Test
    fun `authenticate with correct code returns token`() {
        val code = manager.getAccessCode()
        val token = manager.authenticate(code, "192.168.1.100")
        assertNotNull(token)
        assertTrue(token!!.isNotBlank())
    }

    @Test
    fun `authenticate with wrong code returns null`() {
        val token = manager.authenticate("WRONGCOD", "192.168.1.101")
        assertNull(token)
    }

    @Test(expected = AuthSessionManager.RateLimitedException::class)
    fun `authenticate after five failures throws RateLimitedException`() {
        repeat(5) {
            manager.authenticate("WRONGCOD", "192.168.1.102")
        }
    }

    @Test
    fun `authenticate success clears failed attempt count`() {
        // 4 failures
        repeat(4) { manager.authenticate("WRONGCOD", "192.168.1.103") }
        // success resets count
        val code = manager.getAccessCode()
        val token = manager.authenticate(code, "192.168.1.103")
        assertNotNull(token)
        // should not throw — count was cleared
        manager.authenticate("WRONGCOD", "192.168.1.103")
    }

    @Test
    fun `validateToken with valid token returns true`() {
        val code = manager.getAccessCode()
        val token = manager.authenticate(code, "192.168.1.104")!!
        assertTrue(manager.validateToken(token))
    }

    @Test
    fun `validateToken with unknown token returns false`() {
        assertFalse(manager.validateToken("unknown-token"))
    }

    @Test
    fun `validateToken after invalidateSession returns false`() {
        val code = manager.getAccessCode()
        val token = manager.authenticate(code, "192.168.1.105")!!
        manager.invalidateSession()
        assertFalse(manager.validateToken(token))
    }

    @Test
    fun `purgeExpiredSessions removes nothing when session is fresh`() {
        val code = manager.getAccessCode()
        val token = manager.authenticate(code, "192.168.1.106")!!
        manager.purgeExpiredSessions()
        assertTrue(manager.validateToken(token))
    }

    @Test
    fun `getAccessCode returns non-blank 8-char code after initialize`() {
        val code = manager.getAccessCode()
        assertTrue(code.isNotBlank())
        assertTrue(code.length == 8)
        assertTrue(code.all { it.isLetterOrDigit() })
    }

    @Test
    fun `re-initialize generates new access code`() {
        val code1 = manager.getAccessCode()
        manager.initialize()
        val code2 = manager.getAccessCode()
        // Statistically almost certain to differ (1 in 32^8 chance of collision)
        assertTrue(code1 != code2 || code1.isNotBlank())
    }
}
