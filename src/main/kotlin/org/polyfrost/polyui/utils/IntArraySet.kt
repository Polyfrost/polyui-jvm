package org.polyfrost.polyui.utils

import org.jetbrains.annotations.ApiStatus

/**
 * Simple dynamic array-based [MutableSet] implementation for integers.
 * This class provides basic functionality similar to `HashSet<Int>`, but is optimized for integer storage.
 *
 * @since 1.8.4
 */
@ApiStatus.Experimental
class IntArraySet(initialCapacity: Int) {
    init {
        require(initialCapacity > 0) { "Initial capacity must be greater than 0" }
    }

    private var array: IntArray = IntArray(initialCapacity)
    var size = 0
        private set

    fun isEmpty() = size == 0

    operator fun contains(value: Int): Boolean {
        for (i in 0..<size) {
            if (array[i] == value) return true
        }
        return false
    }

    fun add(value: Int): Boolean {
        if (contains(value)) return false // Avoid duplicates
        if (size == array.size) resize(array.size * 2)
        array[size++] = value
        return true
    }

    fun clear() {
        array.fill(0, 0, size) // Clear the existing elements
        size = 0
    }

    fun remove(value: Int): Int {
        for (i in 0..<size) {
            if (array[i] == value) {
                val removed = array[i]
                array.copyInto(array, i, i + 1, size)
                size--
                array[size] = 0 // Clear the last element
                return removed
            }
        }
        return -1 // Return -1 if the value was not found
    }

    private fun resize(newSize: Int) {
        array = array.copyOf(newSize)
    }

    fun toIntArray(): IntArray {
        val res = IntArray(size)
        array.copyInto(res, endIndex = size)
        return res
    }
}
