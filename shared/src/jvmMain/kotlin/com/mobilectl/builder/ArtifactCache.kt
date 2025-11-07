package com.mobilectl.builder

import java.util.concurrent.ConcurrentHashMap

/**
 * Cache for artifact paths to avoid re-searching between build and deploy phases.
 * Cache is per-flavor-type to handle multiple flavors correctly.
 */
object ArtifactCache {
    private val cache = ConcurrentHashMap<String, String>()

    /**
     * Store artifact path for a specific flavor and type
     */
    fun setArtifactPath(flavor: String, type: String, path: String) {
        val key = getCacheKey(flavor, type)
        cache[key] = path
    }

    /**
     * Get cached artifact path for a specific flavor and type
     */
    fun getArtifactPath(flavor: String, type: String): String? {
        val key = getCacheKey(flavor, type)
        return cache[key]
    }

    /**
     * Clear cache for a specific flavor and type
     */
    fun clearArtifact(flavor: String, type: String) {
        val key = getCacheKey(flavor, type)
        cache.remove(key)
    }

    /**
     * Clear all cached artifacts
     */
    fun clearAll() {
        cache.clear()
    }

    /**
     * Generate cache key from flavor and type
     */
    private fun getCacheKey(flavor: String, type: String): String {
        return "${flavor}_${type}".lowercase()
    }

    /**
     * Check if artifact exists for flavor and type
     */
    fun hasArtifact(flavor: String, type: String): Boolean {
        return getArtifactPath(flavor, type) != null
    }
}
