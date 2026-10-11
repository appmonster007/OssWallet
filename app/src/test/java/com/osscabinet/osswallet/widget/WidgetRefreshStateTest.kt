package com.osscabinet.ossWallet.widget

import com.osscabinet.ossWallet.widget.WidgetRefreshState.Pass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetRefreshStateTest {

    private val state = WidgetRefreshState()

    @Test
    fun firstRequestClaimsWorker_laterRequestsDoNot() {
        assertTrue(state.requestAll())
        assertFalse(state.requestAll())
        assertFalse(state.requestWidgets(listOf(1)))
    }

    @Test
    fun burstOfRequestsCoalescesIntoOnePass() {
        state.requestAll()
        repeat(4) { state.requestAll() }

        val pass = state.beginPass()
        assertTrue(pass is Pass.All)
        state.complete(pass!!)

        assertNull(state.beginPass())
    }

    @Test
    fun writeDuringPassTriggersAnotherPass() {
        state.requestAll()
        val first = state.beginPass()!!
        state.requestAll() // lands after the pass snapshotted its version
        state.complete(first)

        val second = state.beginPass()
        assertTrue(second is Pass.All)
        assertTrue((second as Pass.All).version > (first as Pass.All).version)
    }

    @Test
    fun requestDuringPassDoesNotReclaimWorker() {
        state.requestAll()
        state.beginPass()
        assertFalse(state.requestAll())
    }

    @Test
    fun requestAfterWorkerRetiresClaimsAgain() {
        state.requestAll()
        state.complete(state.beginPass()!!)
        assertNull(state.beginPass()) // worker retires, claim released

        assertTrue(state.requestAll())
    }

    @Test
    fun widgetRequestsOnlyRenderThoseWidgets() {
        state.requestWidgets(listOf(7))
        state.requestWidgets(listOf(9))

        val pass = state.beginPass()
        assertEquals(Pass.Widgets(setOf(7, 9)), pass)
        state.complete(pass!!)
        assertNull(state.beginPass())
    }

    @Test
    fun widgetRequestDoesNotInvalidateFullPass() {
        state.requestAll()
        val all = state.beginPass()!!
        state.requestWidgets(listOf(3)) // resize mid-pass
        state.complete(all)

        assertEquals(Pass.Widgets(setOf(3)), state.beginPass())
    }

    @Test
    fun fullPassCoversQueuedWidgetRequests() {
        state.requestWidgets(listOf(3))
        state.requestAll()

        assertTrue(state.beginPass() is Pass.All)
        state.complete(Pass.All(1))
        assertNull(state.beginPass())
    }

    @Test
    fun abortedFullPassIsStillOwed() {
        state.requestAll()
        val pass = state.beginPass()!!
        state.abort(pass)

        assertTrue(state.requestWidgets(listOf(1))) // claim released
        assertTrue(state.beginPass() is Pass.All)
    }

    @Test
    fun abortedWidgetPassIsRequeued() {
        state.requestWidgets(listOf(5))
        val pass = state.beginPass()!!
        state.abort(pass)
        state.attach() // WorkManager retry

        assertEquals(Pass.Widgets(setOf(5)), state.beginPass())
    }

    @Test
    fun emptyWidgetRequestIsIgnored() {
        assertFalse(state.requestWidgets(emptyList()))
        assertNull(state.beginPass())
    }
}
