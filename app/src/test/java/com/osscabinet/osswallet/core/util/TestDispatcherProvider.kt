package com.osscabinet.ossWallet.core.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher

/**
 * Test double for [DispatcherProvider]. Routes every dispatcher to a single
 * [TestDispatcher] so coroutine execution is deterministic and driven by the
 * test scheduler. Inject this into repositories/services under test instead of
 * relying on the real [Dispatchers] (AD-9).
 *
 * No default is provided on purpose: callers MUST pass the test's own dispatcher
 * (e.g. `StandardTestDispatcher(testScheduler)` inside `runTest`). A self-created
 * default dispatcher would carry its own scheduler that `advanceUntilIdle()` cannot
 * drive, so coroutines would never run and tests would hang or silently pass.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TestDispatcherProvider(
    private val dispatcher: TestDispatcher
) : DispatcherProvider {
    override val main: CoroutineDispatcher = dispatcher
    override val io: CoroutineDispatcher = dispatcher
    override val default: CoroutineDispatcher = dispatcher
    override val unconfined: CoroutineDispatcher = dispatcher
}
