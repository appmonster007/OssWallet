package com.osscabinet.ossWallet.domain.util

/**
 * Performs a simple fuzzy match. Returns true if characters of [query] 
 * appear in this string in the same order.
 */
fun String.fuzzyMatch(query: String): Boolean {
    if (query.isEmpty()) return true
    var queryIdx = 0
    val lowerQuery = query.lowercase()
    val lowerThis = this.lowercase()
    
    for (char in lowerThis) {
        if (char == lowerQuery[queryIdx]) {
            queryIdx++
            if (queryIdx == lowerQuery.length) return true
        }
    }
    return false
}
