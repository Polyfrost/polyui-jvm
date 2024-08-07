/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023-2024 Polyfrost and its contributors.
 *   <https://polyfrost.org> <https://github.com/Polyfrost/polui-jvm>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *     PolyUI is licensed under the terms of version 3 of the GNU Lesser
 * General Public License as published by the Free Software Foundation,
 * AND the simple request that you adequately accredit us if you use PolyUI.
 * See details here <https://github.com/Polyfrost/polyui-jvm/ACCREDITATION.md>.
 *     This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 * License.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.polyfrost.polyui.utils

/**
 * Simple map for Int to Int relations. Designed for small to medium maps with a low amount of entries (<1000).
 *
 * As there is no way to specify invalid values, **a return value of `0` is given when the mapping does not exist**.
 * While it does mostly comply to the [MutableMap] interface, it is **not implemented** for performance reasons and for simplicity.
 * It is designed to be used as a drop in replacement for a [Map] when the key is an [Int] and the value is an [Int].
 *
 * Implemented as a dual sorted array with binary search for fast lookups. Here is a summary of the performance characteristics:
 * ### `get(key: Int)`:
 * - Best/Average Case: 𝑂(log 𝑛). Searching is performed using a binary algorithm.
 * ### `set(key: Int, value: Int)`:
 * - Best/Average Case:
 * 𝑂(log 𝑛) for the binary search and 𝑂(1) for the insertion without resizing.
 * - Worst Case:
 * 𝑂(log 𝑛) for the binary search and 𝑂(𝑛) for the insertion with array shifting *and* resizing.
 */
class Int2IntMap(initialCapacity: Int) {
    init {
        require(initialCapacity > 0) { "Initial capacity must be greater than 0" }
    }

    @PublishedApi
    internal var keys: IntArray = IntArray(initialCapacity)

    @PublishedApi
    internal var values: IntArray = IntArray(initialCapacity)
    var size = 0
        private set

    fun isEmpty() = size == 0

    /**
     * Get the value associated with the given key.
     * @return the value associated with the key, or `0` if the key was not present.
     */
    operator fun get(key: Int): Int {
        if (size == 0) return 0
        val index = keys.binarySearch(key, 0, size)
        return if (index >= 0) values[index] else 0
    }

    operator fun set(key: Int, value: Int) {
        if (size == 0) { // opt: fast-path when map is empty
            keys[0] = key
            values[0] = value
            size = 1
            return
        }
        var index = keys.binarySearch(key, 0, size)
        if (index >= 0) {
            values[index] = value
        } else {
            index = -index - 1
            if (size == keys.size) resize(keys.size * 2)

            if (index == size) { // opt: avoid arraycopy when it is an effective append
                keys[size] = key
                values[size] = value
            } else {
                keys.copyInto(keys, index + 1, index, size)
                values.copyInto(values, index + 1, index, size)
                keys[index] = key
                values[index] = value
            }
            size++
        }
    }

    fun put(key: Int, value: Int) {
        this[key] = value
    }

    operator fun contains(key: Int) = containsKey(key)

    fun containsKey(key: Int) = if (size == 0) false else keys.binarySearch(key, 0, size) >= 0

    fun containsValue(value: Int): Boolean {
        for (i in 0 until size) {
            if (values[i] == value) return true
        }
        return false
    }

    /**
     * Remove the mapping for the given key.
     * @return the previous value associated with the key, or `0` if the key was not present.
     */
    fun remove(key: Int): Int {
        if (size == 0) return 0
        val index = keys.binarySearch(key, 0, size)
        if (index < 0) return 0
        val old = values[index]
        keys.copyInto(keys, index, index + 1, size)
        values.copyInto(values, index, index + 1, size)
        size--
        return old
    }

    private fun resize(to: Int) {
        if (keys.size == to) return
        keys = keys.copyOf(to)
        values = values.copyOf(to)
    }

    /**
     * Trim the internal arrays to the current size. This is useful when the map will not be modified anymore.
     *
     * Requires a full copy of the arrays, so it is not recommended to be used frequently. It also means next insertion will require another copy.
     */
    fun trim() = resize(size)

    /**
     * Clear the map. **This does not actually discard any data for performance reasons.**
     */
    fun clear() {
        size = 0
    }

    inline fun getOrPut(key: Int, defaultValue: () -> Int): Int {
        val index = if (size == 0) -1 else keys.binarySearch(key, 0, size)
        return if (index >= 0) {
            values[index]
        } else {
            val value = defaultValue()
            this[key] = value
            value
        }
    }

    inline fun forEach(action: (Int, Int) -> Unit) {
        for (i in 0 until size) action(keys[i], values[i])
    }

    operator fun iterator() = Iter()

    inner class Iter {
        private var i = 0
        operator fun hasNext() = i < size
        @JvmName("next")
        operator fun next(): Entry {
            val i = i
            if (i >= size) throw NoSuchElementException()
            this.i++
            return Entry((keys[i].toLong() shl 32) or (values[i].toLong() and 0xFFFFFFFFL))
        }
    }

    @JvmInline
    value class Entry(private val it: Long) {
        val key get() = (it shr 32).toInt()
        val value get() = it.toInt()

        operator fun component1() = key
        operator fun component2() = value
    }
}
