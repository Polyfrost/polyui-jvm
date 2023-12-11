/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors.
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

@file:Suppress("invisible_member", "invisible_reference")

package org.polyfrost.polyui.utils

import org.jetbrains.annotations.ApiStatus

/**
 * Implementation of the [MutableList] interface for PolyUI.
 *
 * This is a minor-ly modified version of [java.util.LinkedList], with "fast" methods:
 * - [fastEach]
 * - [fastEachIndexed]
 * - [fastEachReversed]
 * - [fastRemoveIf]
 * - [fastRemoveIfIndexed]
 * - [fastRemoveIfReversed]
 * these methods are all designed to avoid the allocation of an Iterator object, as otherwise PolyUI would create a lot of garbage.
 *
 * Being a LinkedList, the random get and put operations are not the best. However, this operation is rare in PolyUI, so it's okay to use this.
 */
class LinkedList<T>() : MutableList<T> {
    constructor(elements: Collection<T>) : this() {
        addAll(elements)
    }

    constructor(vararg elements: T) : this() {
        for (element in elements) add(element)
    }

    var start: Node<T>? = null
        private set
    var end: Node<T>? = null
        private set

    override var size: Int = 0
        private set

    override fun addAll(index: Int, elements: Collection<T>): Boolean {
        val current = _get(index)
        for (element in elements) {
            val n = Node(element, current, current.prev)
            current.prev?.next = n
            current.prev = n
            size++
        }
        return true
    }

    override fun clear() {
        start = null
        end = null
        size = 0
    }

    override fun addAll(elements: Collection<T>): Boolean {
        if (elements is LinkedList) {
            this.end?.next = elements.start
            elements.start?.prev = this.end
            this.end = elements.end
            this.size += elements.size
        } else {
            for (element in elements) add(element)
        }
        return true
    }

    override fun add(index: Int, element: T) {
        when (index) {
            0 -> {
                val n = Node(element, start)
                start?.prev = n
                start = n
                size++
            }

            size -> {
                val n = Node(element, null, end)
                end?.next = n
                end = n
                size++
            }

            else -> {
                val it = _get(index)
                val n = Node(element, it, it.prev)
                it.prev?.next = n
                it.prev = n
                size++
            }
        }
    }

    override fun add(element: T): Boolean {
        val n = Node(element)
        if (start == null) {
            start = n
            end = n
        } else {
            end?.next = n
            n.prev = end
            end = n
        }
        size++
        return true
    }

    override fun isEmpty() = start == null && end == null

    @Suppress("Deprecation")
    @Deprecated("This list is designed around not using this method.", ReplaceWith("fastEach"))
    override fun iterator() = listIterator(0)

    @Suppress("Deprecation")
    @Deprecated("This list is designed around not using this method.", ReplaceWith("fastEach"))
    override fun listIterator() = listIterator(0)

    @Deprecated("This list is designed around not using this method.", ReplaceWith("fastEach"))
    override fun listIterator(index: Int): MutableListIterator<T> {
        return object : MutableListIterator<T> {
            var current: Node<T>? = _get(index)
            var idx = index - 1
            override fun hasNext() = current != null

            override fun next(): T {
                val n = current ?: throw NoSuchElementException()
                current = n.next
                idx++
                return n.value
            }

            override fun hasPrevious() = current?.prev != null

            override fun previous(): T {
                val n = current?.prev ?: throw NoSuchElementException()
                current = n
                idx--
                return n.value
            }

            override fun nextIndex() = idx + 1

            override fun previousIndex() = idx - 1

            override fun add(element: T) {
                val n = Node(element, current, current?.prev)
                current?.prev?.next = n
                current?.prev = n
                size++
            }

            override fun remove() {
                val n = current ?: throw NoSuchElementException()
                if (current === end) end = n.prev
                else if (current === start) start = n.next
                current = n.next
                n.prev?.next = n.next
                n.next?.prev = n.prev
                size--
            }

            override fun set(element: T) {
                val n = current?.prev ?: end ?: throw NullPointerException("list empty?")
                n.value = element
            }
        }
    }

    override fun removeAt(index: Int): T {
        val n = _get(index)
        _remove(n)
        return n.value
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<T> {
        val out = LinkedList<T>()
        out.start = _get(fromIndex)
        out.end = _get(toIndex)
        return out
    }

    override fun lastIndexOf(element: T): Int {
        var current = end
        var i = size - 1
        while (current != null) {
            if (current.value == element) return i
            current = current.prev
            i--
        }
        return -1
    }

    override fun retainAll(elements: Collection<T>): Boolean {
        var current = start
        while (current != null) {
            if (current.value !in elements) {
                _remove(current)
            }
            current = current.next
        }
        return false
    }

    override fun removeAll(elements: Collection<T>): Boolean {
        var current = start
        while (current != null) {
            if (current.value in elements) {
                _remove(current)
            }
            current = current.next
        }
        return true
    }

    override fun remove(element: T): Boolean {
        var current = start
        while (current != null) {
            if (current.value == element) {
                _remove(current)
                return true
            }
            current = current.next
        }
        return false
    }

    override fun containsAll(elements: Collection<T>): Boolean {
        for (element in elements) {
            if (!contains(element)) return false
        }
        return true
    }

    override operator fun get(index: Int) = _get(index).value

    override fun indexOf(element: T): Int {
        var current = start
        var i = 0
        while (current != null) {
            if (current.value == element) return i
            current = current.next
            i++
        }
        return -1
    }

    override operator fun set(index: Int, element: T): T {
        val it = _get(index)
        val old = it.value
        it.value = element
        return old
    }

    override fun contains(element: T): Boolean {
        var current = start
        while (current != null) {
            if (current.value == element) return true
            current = current.next
        }
        return false
    }

    @kotlin.internal.InlineOnly
    inline fun fastEach(action: (T) -> Unit) {
        var current = start
        while (current != null) {
            action(current.value)
            current = current.next
        }
    }

    @kotlin.internal.InlineOnly
    inline fun fastEachIndexed(action: (Int, T) -> Unit) {
        var current = start
        var i = 0
        while (current != null) {
            action(i, current.value)
            current = current.next
            i++
        }
    }
    @kotlin.internal.InlineOnly
    inline fun fastEachReversed(action: (T) -> Unit) {
        var current = end
        while (current != null) {
            action(current.value)
            current = current.prev
        }
    }

    @kotlin.internal.InlineOnly
    inline fun fastRemoveIf(predicate: (T) -> Boolean): Boolean {
        var current = start
        var out = false
        while (current != null) {
            if (predicate(current.value)) {
                _remove(current)
                out = true
            }
            current = current.next
        }
        return out
    }

    @kotlin.internal.InlineOnly
    inline fun fastRemoveIfIndexed(predicate: (Int, T) -> Boolean): Boolean {
        var current = start
        var i = 0
        var out = false
        while (current != null) {
            if (predicate(i, current.value)) {
                _remove(current)
                out = true
            }
            current = current.next
            i++
        }
        return out
    }

    @kotlin.internal.InlineOnly
    inline fun fastRemoveIfReversed(predicate: (T) -> Boolean): Boolean {
        var current = end
        var out = false
        while (current != null) {
            if (predicate(current.value)) {
                _remove(current)
                out = true
            }
            current = current.prev
        }
        return out
    }

    fun addOrReplace(element: T): T? {
        var current = start
        while (current != null) {
            if (current.value == element) {
                current.value = element
                return element
            }
            current = current.next
        }
        add(element)
        return null
    }

    @ApiStatus.Internal
    fun _remove(node: Node<T>) {
        if (node === start) start = node.next
        if (node === end) end = node.prev
        node.prev?.next = node.next
        node.next?.prev = node.prev
        size--
    }

    /**
     * Returns `true` if **all elements** match the given [predicate].
     *
     * **Note this function** will return `true` if the list is empty, according to the principle of [Vacuous truth](https://en.wikipedia.org/wiki/Vacuous_truth),
     * and the fact that there are no elements that don't match the given predicate.
     */
    @kotlin.internal.InlineOnly
    inline fun allAre(predicate: (T) -> Boolean): Boolean {
        var current = start
        while (current != null) {
            if (!predicate(current.value)) return false
            current = current.next
        }
        return true
    }

    private fun _get(index: Int): Node<T> {
        val diff = size - index
        if (diff < index) {
            var current = end
            var i = size - 1
            while (current != null) {
                if (i == index) return current
                current = current.prev
                i--
            }
            throw IndexOutOfBoundsException("Index: $index, Size: $size")
        } else {
            var current = start
            var i = 0
            while (current != null) {
                if (i == index) return current
                current = current.next
                i++
            }
            throw IndexOutOfBoundsException("Index: $index, Size: $size")
        }
    }

    @kotlin.internal.InlineOnly
    fun first(): T = start?.value ?: throw NoSuchElementException()

    @kotlin.internal.InlineOnly
    fun last(): T = end?.value ?: throw NoSuchElementException()

    @ApiStatus.Internal
    class Node<T>(var value: T, var next: Node<T>? = null, var prev: Node<T>? = null)
}
