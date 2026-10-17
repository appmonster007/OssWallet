package com.osscabinet.ossWallet.ui.screens.filters

import com.osscabinet.ossWallet.core.util.MainDispatcherRule
import com.osscabinet.ossWallet.domain.model.Filter
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.test.FakeFilterRepository
import com.osscabinet.ossWallet.test.FakeSettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FilterViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private val repo = FakeFilterRepository()
    private val settings = FakeSettingsRepository(initialProfileId = 1)

    private suspend fun seed() {
        repo.insertFilter(Filter(name = "Mine", filter = TransactionFilter(), profileId = 1))
        repo.insertFilter(Filter(name = "Theirs", filter = TransactionFilter(), profileId = 2))
    }

    @Test fun `the list shows only the current profile's filters and follows a profile switch`() = runTest(mainRule.testDispatcher) {
        seed()
        val vm = FilterViewModel(repo, settings)
        backgroundScope.launch { vm.allFilters.collect {} }
        advanceUntilIdle()
        assertEquals(listOf("Mine"), vm.allFilters.value.map { it.name })

        settings.saveCurrentProfileId(2)
        advanceUntilIdle()
        assertEquals(listOf("Theirs"), vm.allFilters.value.map { it.name })

        settings.saveCurrentProfileId(3)
        advanceUntilIdle()
        assertTrue(vm.allFilters.value.isEmpty())
    }

    @Test fun `a duplicate stays in the profile of the original`() = runTest(mainRule.testDispatcher) {
        seed()
        settings.saveCurrentProfileId(2)
        val vm = FilterViewModel(repo, settings)
        backgroundScope.launch { vm.allFilters.collect {} }
        advanceUntilIdle()

        vm.duplicateFilter(vm.allFilters.value.single())
        advanceUntilIdle()
        assertEquals(listOf("Theirs", "Theirs copy"), vm.allFilters.value.map { it.name })
        assertTrue(vm.allFilters.value.all { it.profileId == 2 })

        settings.saveCurrentProfileId(1)
        advanceUntilIdle()
        assertEquals(listOf("Mine"), vm.allFilters.value.map { it.name })
    }

    @Test fun `deleting removes only that filter`() = runTest(mainRule.testDispatcher) {
        seed()
        val vm = FilterViewModel(repo, settings)
        backgroundScope.launch { vm.allFilters.collect {} }
        advanceUntilIdle()

        vm.deleteFilter(vm.allFilters.value.single())
        advanceUntilIdle()
        assertTrue(vm.allFilters.value.isEmpty())
        settings.saveCurrentProfileId(2)
        advanceUntilIdle()
        assertEquals(listOf("Theirs"), vm.allFilters.value.map { it.name })
    }
}
