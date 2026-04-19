package com.osscabinet.ossWallet.ui.widget

import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*
import java.util.concurrent.atomic.AtomicInteger

class WidgetUpdateFlowTest {

    /**
     * This test simulates the "Rapid Creation" issue.
     * It demonstrates how a refresh sequence can be interrupted if not protected by NonCancellable,
     * leading to some widgets not being updated (the root cause of the random no-update issue).
     */
    @Test
    fun `test rapid triggers without protection leads to partial updates`() = runTest {
        val updatedWidgetsCount = AtomicInteger(0)
        val totalWidgets = 5
        
        // Simulating the performUpdate loop WITHOUT NonCancellable protection
        suspend fun unprotectedPerformUpdate() {
            for (i in 1..totalWidgets) {
                delay(100) // Simulate work (e.g., DB query + Glance session start)
                updatedWidgetsCount.incrementAndGet()
            }
        }

        val job = launch {
            try {
                unprotectedPerformUpdate()
            } catch (e: CancellationException) {
                // Expected when replaced
            }
        }

        // Simulate a second trigger coming in quickly (e.g., user added another transaction)
        delay(250) 
        job.cancel() // ExistingWorkPolicy.REPLACE cancels the previous worker

        // At this point, only ~2 widgets would have updated
        assertTrue("Unprotected update should have been partial", updatedWidgetsCount.get() < totalWidgets)
        println("Unprotected update stopped at ${updatedWidgetsCount.get()} / $totalWidgets")
    }

    /**
     * This test demonstrates the ROBUST solution.
     * Even if the Job is cancelled (replaced), the internal update sequence completes.
     */
    @Test
    fun `test robust update completes even if job is cancelled`() = runTest {
        val updatedWidgetsCount = AtomicInteger(0)
        val totalWidgets = 5
        
        // Simulating the performUpdate loop WITH NonCancellable protection
        suspend fun robustPerformUpdate() {
            withContext(NonCancellable) {
                for (i in 1..totalWidgets) {
                    delay(100)
                    updatedWidgetsCount.incrementAndGet()
                }
            }
        }

        val job = launch {
            robustPerformUpdate()
        }

        delay(250)
        job.cancel() // Simulate REPLACE cancellation
        
        // Join the job to ensure it finished its NonCancellable block
        job.join()

        assertEquals("Robust update should complete all widgets", totalWidgets, updatedWidgetsCount.get())
        println("Robust update finished at ${updatedWidgetsCount.get()} / $totalWidgets")
    }
}
