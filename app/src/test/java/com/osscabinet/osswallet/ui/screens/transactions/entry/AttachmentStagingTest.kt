package com.osscabinet.ossWallet.ui.screens.transactions.entry

import com.osscabinet.ossWallet.domain.model.AttachmentLimits
import com.osscabinet.ossWallet.test.FakeAttachmentRepository
import com.osscabinet.ossWallet.test.FakeAttachmentProcessor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AttachmentStagingTest {

    private val repo = FakeAttachmentRepository()

    private fun TestScope.staging(): AttachmentStaging {
        // backgroundScope: the staging keeps long-lived collectors that would otherwise stop runTest finishing.
        val s = AttachmentStaging(repo, FakeAttachmentProcessor(), backgroundScope)
        s.state.launchIn(backgroundScope) // keep the WhileSubscribed state hot
        return s
    }

    @Test
    fun `picked files are staged and only stored on commit`() = runTest(UnconfinedTestDispatcher()) {
        val s = staging()
        s.add("one"); s.add("two"); advanceUntilIdle()

        assertEquals(2, s.state.value.items.size)
        assertTrue(repo.getForTransaction(9, 1).isEmpty())

        s.commit(9, 1)
        assertEquals(2, repo.getForTransaction(9, 1).size)
        assertTrue(s.state.value.items.isEmpty() || s.state.value.items.all { it.attachment == null })
    }

    @Test
    fun `abandoning the form stores nothing`() = runTest(UnconfinedTestDispatcher()) {
        val s = staging()
        s.add("one"); advanceUntilIdle()
        // no commit
        assertTrue(repo.getForTransaction(9, 1).isEmpty())
    }

    @Test
    fun `removing a stored attachment is deferred until commit`() = runTest(UnconfinedTestDispatcher()) {
        val stored = repo.add(9, 1, "x".toByteArray())
        val s = staging()
        s.load(9, 1); advanceUntilIdle()
        assertEquals(1, s.state.value.items.size)

        s.remove(s.state.value.items.single()); advanceUntilIdle()
        assertTrue("hidden from the form immediately", s.state.value.items.isEmpty())
        assertEquals("still stored until saved", 1, repo.getForTransaction(9, 1).size)

        s.commit(9, 1); advanceUntilIdle()
        assertTrue(repo.getForTransaction(9, 1).isEmpty())
        assertNotNull(stored)
    }

    @Test
    fun `removing a staged file drops it without touching storage`() = runTest(UnconfinedTestDispatcher()) {
        val s = staging()
        s.add("one"); advanceUntilIdle()
        s.remove(s.state.value.items.single()); advanceUntilIdle()

        assertTrue(s.state.value.items.isEmpty())
        s.commit(9, 1)
        assertTrue(repo.getForTransaction(9, 1).isEmpty())
    }

    @Test
    fun `an unusable file surfaces an error and adds nothing`() = runTest(UnconfinedTestDispatcher()) {
        val s = staging()
        s.add("bad-file"); advanceUntilIdle()

        assertTrue(s.state.value.items.isEmpty())
        assertEquals("That file is larger than 20 MB", s.state.value.error)
        s.clearError(); advanceUntilIdle()
        assertNull(s.state.value.error)
    }

    @Test
    fun `stored plus staged files count against the cap`() = runTest(UnconfinedTestDispatcher()) {
        repeat(AttachmentLimits.MAX_PER_TRANSACTION - 1) { repo.add(9, 1, byteArrayOf(1)) }
        val s = staging()
        s.load(9, 1); advanceUntilIdle()
        s.add("last"); advanceUntilIdle()
        assertEquals(AttachmentLimits.MAX_PER_TRANSACTION, s.state.value.items.size)

        s.add("too many"); advanceUntilIdle()
        assertEquals(AttachmentLimits.MAX_PER_TRANSACTION, s.state.value.items.size)
        assertTrue(s.state.value.error!!.contains("at most"))
        assertTrue(!s.state.value.canAdd)
    }

    @Test
    fun `a new transaction starts with no attachments`() = runTest(UnconfinedTestDispatcher()) {
        val s = staging()
        s.load(null, 1); advanceUntilIdle()
        assertTrue(s.state.first().items.isEmpty())
    }

    @Test
    fun `non-image files keep their name and type through commit`() = runTest(UnconfinedTestDispatcher()) {
        val s = staging()
        s.add("tax-invoice.pdf"); advanceUntilIdle()
        val item = s.state.value.items.single()
        assertEquals("tax-invoice.pdf", item.displayName)
        assertTrue(!item.isImage)

        s.commit(9, 1)
        val stored = repo.getForTransaction(9, 1).single()
        assertEquals("application/pdf", stored.mimeType)
        assertEquals("tax-invoice.pdf", stored.displayName)
        assertTrue(!stored.isImage)
    }
}
