package com.osscabinet.ossWallet.core.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DispatcherProviderTest {

    @Test
    fun `RealDispatcherProvider maps each role to the matching Dispatchers value`() {
        val provider = RealDispatcherProvider()

        assertSame(Dispatchers.Main, provider.main)
        assertSame(Dispatchers.IO, provider.io)
        assertSame(Dispatchers.Default, provider.default)
        assertSame(Dispatchers.Unconfined, provider.unconfined)
    }

    @Test
    fun `TestDispatcherProvider routes every role to the same injected test dispatcher`() {
        val dispatcher = StandardTestDispatcher()
        val provider = TestDispatcherProvider(dispatcher)

        assertSame(dispatcher, provider.main)
        assertSame(dispatcher, provider.io)
        assertSame(dispatcher, provider.default)
        assertSame(dispatcher, provider.unconfined)
    }

    @Test
    fun `work dispatched on the injected io dispatcher runs under the test scheduler`() = runTest {
        val provider = TestDispatcherProvider(StandardTestDispatcher(testScheduler))

        // Mirrors the repository pattern: suspend work wrapped in withContext(dispatcherProvider.io).
        val result = withContext(provider.io) { "ran-on-injected-dispatcher" }

        assertEquals("ran-on-injected-dispatcher", result)
    }
}
