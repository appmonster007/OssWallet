package com.osscabinet.ossWallet.domain.service.category

import com.osscabinet.ossWallet.domain.model.Category

/**
 * Pre-computed, immutable hierarchy map for categories.
 * Built once per category list change; consumed by card computers and filter logic via O(1) lookups.
 *
 * [descendantMap] maps each category ID to the full set of all its descendant IDs (not including itself).
 */
data class CategoryHierarchyCache(
    val descendantMap: Map<Long, Set<Long>>
) {
    /**
     * Expands [ids] to include each ID plus all its descendants.
     * Returns the same set if [ids] is empty or nothing needs expanding.
     */
    fun expand(ids: Set<Long>): Set<Long> {
        if (ids.isEmpty()) return emptySet()
        val result = mutableSetOf<Long>()
        for (id in ids) {
            result.add(id)
            descendantMap[id]?.let { result.addAll(it) }
        }
        return result
    }

    companion object {
        val Empty = CategoryHierarchyCache(emptyMap())

        // Single-slot memo: `build` is called fresh inside DashboardViewModel's (and
        // AnalyticsViewModel's) combine() mapper every time that flow restarts — e.g. every
        // WhileSubscribed(5000) resubscription when a user navigates back to a screen after
        // a 5s+ gap — even though the category list is usually unchanged since last time.
        // Memoizing the last (input -> result) pair turns that repeat case into an equals()
        // check instead of a full BFS rebuild. Benign under concurrent access from multiple
        // screens' flows: worst case on a race is a wasted recompute, never a wrong result,
        // since this is a pure function of `categories`.
        @Volatile
        private var lastCategories: List<Category>? = null

        @Volatile
        private var lastResult: CategoryHierarchyCache? = null

        /**
         * Builds the cache from a flat category list using iterative BFS per root.
         * O(N²) worst case but N is small (<500 categories); memoized against the
         * previous call so an unchanged category list is just an equals() check.
         */
        fun build(categories: List<Category>): CategoryHierarchyCache {
            if (categories.isEmpty()) return Empty

            lastResult?.let { cached ->
                if (categories == lastCategories) return cached
            }

            // Index children by parentId for fast lookup
            val childrenByParent = categories.groupBy { it.parentId }

            fun collectDescendants(id: Long): Set<Long> {
                val result = mutableSetOf<Long>()
                val queue = ArrayDeque<Long>()
                childrenByParent[id]?.forEach { queue.add(it.id) }
                while (queue.isNotEmpty()) {
                    val current = queue.removeFirst()
                    if (result.add(current)) {
                        childrenByParent[current]?.forEach { queue.add(it.id) }
                    }
                }
                return result
            }

            val map = categories.associate { cat -> cat.id to collectDescendants(cat.id) }
            val result = CategoryHierarchyCache(map)
            lastCategories = categories
            lastResult = result
            return result
        }
    }
}
