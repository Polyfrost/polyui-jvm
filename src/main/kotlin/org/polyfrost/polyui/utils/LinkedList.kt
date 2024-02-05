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

package org.polyfrost.polyui.utils

import org.jetbrains.annotations.ApiStatus

/**
 * Implementation of the [MutableList] interface for PolyUI.
 *
 * This is a doubly-linked list, with special "fast" methods:
 * - [fastEach]
 * - [fastEachIndexed]
 * - [fastEachReversed]
 * - [fastRemoveIf]
 * - [fastRemoveIfIndexed]
 * - [fastRemoveIfReversed]
 *
 * these methods are all designed to avoid the allocation of an Iterator object, as otherwise PolyUI would create a lot of garbage.
 *
 * this collection, unlike most of the collections in the Java Collections Framework, does **not** make any effort to check against [concurrent modification][ConcurrentModificationException].
 * this allows you to do some "quirky" things, such as iterating over the list and removing elements at the same time.
 *
 * Being a linked list, the random get and put operations are not the best. However, this operation is rare in PolyUI, so it's okay to use this.
 * @since 1.0.0
 * @see java.util.LinkedList
 */
class LinkedList<T>() : MutableList<T>, Cloneable {
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

    override fun clear() = clear(unlink = false)

    fun clear(unlink: Boolean) {
        if (unlink) {
            var current = start
            while (current != null) {
                val n = current.next
                current.next = null
                current.prev = null
                current = n
            }
        }
        start = null
        end = null
        size = 0
    }

    override fun addAll(elements: Collection<T>): Boolean {
        if (elements is LinkedList<T>) {
            elements.fastEach {
                add(it)
            }
        } else {
            for (element in elements) {
                add(element)
            }
        }
        return true
    }

    fun addAll(elements: Array<out T>) {
        for (element in elements) {
            add(element)
        }
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
        if (size == 0) {
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

    override fun isEmpty() = size == 0

    @Deprecated("This list is designed around not using this method.", ReplaceWith("fastEach"))
    override fun iterator(): MutableListIterator<T> = LLIterator(this, 0)

    @Deprecated("This list is designed around not using this method.", ReplaceWith("fastEach"))
    override fun listIterator(): MutableListIterator<T> = LLIterator(this, 0)

    @Deprecated("This list is designed around not using this method.", ReplaceWith("fastEach"))
    override fun listIterator(index: Int): MutableListIterator<T> = LLIterator(this, index)

    override fun removeAt(index: Int): T {
        val n = _get(index)
        _remove(n)
        return n.value
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<T> {
        val out = LinkedList<T>()
        var i = fromIndex
        var current = _get(fromIndex)
        while (i < toIndex) {
            out.add(current.value)
            current = current.next ?: throw IndexOutOfBoundsException("Index: $i, Size: $size")
            i++
        }
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
        var out = false
        while (current != null) {
            if (current.value !in elements) {
                _remove(current)
                out = true
            }
            current = current.next
        }
        return out
    }

    override fun removeAll(elements: Collection<T>): Boolean {
        var current = start
        var out = false
        while (current != null) {
            if (current.value in elements) {
                _remove(current)
                out = true
            }
            current = current.next
        }
        return out
    }

    override fun remove(element: T): Boolean {
        if (end?.value == element) {
            _remove(end ?: return false)
            return true
        }
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

    override fun contains(element: T) = indexOf(element) != -1

        inline fun fastEach(action: (T) -> Unit) {
        var current = start
        while (current != null) {
            action(current.value)
            current = current.next
        }
    }

        inline fun fastEachIndexed(action: (Int, T) -> Unit) {
        var current = start
        var i = 0
        while (current != null) {
            action(i, current.value)
            current = current.next
            i++
        }
    }

        inline fun fastEachReversed(action: (T) -> Unit) {
        var current = end
        while (current != null) {
            action(current.value)
            current = current.prev
        }
    }

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

        inline fun clearing(function: (T) -> Unit): Boolean {
        if (size == 0) return false
        var current = start
        while (current != null) {
            function(current.value)
            current = current.next
        }
        clear()
        return true
    }

    /**
     * Adds or replaces an element in the list. If the element is already in the list (using [equals]), it will be replaced, otherwise it will be added.
     *
     * If it is replaced, the old element will be returned, otherwise `null` will be returned.
     *
     * @param element the element to add or replace. `null` is disallowed by the `T & Any` type.
     */
    fun addOrReplace(element: T & Any): T? {
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

    /**
     * Sublist this list in the range of [from] to [to], discarding any elements outside of that range.
     *
     * This function is effectively equal to [subList], but it effects this list, rather than returning a new one.
     *
     * @param from the start index, must be more than 0 and less than [to]
     * @param to the end index, must be less than [size] and more than [from]
     */
    fun cut(from: Int, to: Int) {
        require(from < to) { "from must be less than to ($from..$to)" }
        val start = _get(from)
        val end = _get(to)
        this.start = start
        this.end = end
        start.prev = null
        end.next = null
        size = to - from + 1
    }

    /**
     * Remove a node from a LinkedList.
     * Note that if the node does not belong in this list, the behaviour is undefined.
     */
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
        inline fun allAre(predicate: (T) -> Boolean): Boolean {
        var current = start
        while (current != null) {
            if (!predicate(current.value)) return false
            current = current.next
        }
        return true
    }

    private fun _get(index: Int): Node<T> {
        if (index < 0) throw IndexOutOfBoundsException("Index: $index, Size: $size")
        if (index >= size) throw IndexOutOfBoundsException("Index: $index, Size: $size")
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

        inline fun first(): T = start?.value ?: throw NoSuchElementException("list is empty.")

        inline fun firstOrNull(): T? = start?.value

        inline fun first(predicate: (T) -> Boolean): T {
        var current = start
        while (current != null) {
            if (predicate(current.value)) return current.value
            current = current.next
        }
        throw NoSuchElementException("No element found matching the given predicate.")
    }

        inline fun last(): T = end?.value ?: throw NoSuchElementException("list is empty.")

        inline fun lastOrNull(): T? = end?.value

    override fun equals(other: Any?): Boolean {
        if (other !is Collection<*>) return false
        if (other.size != size) return false
        var current = start
        for (value in other) {
            if (current?.value != value) return false
            current = current?.next ?: return false
        }
        return true
    }

    override fun hashCode(): Int {
        var current = start
        var i = 0
        var out = 0
        while (current != null) {
            out += current.value.hashCode() * i
            current = current.next
            i++
        }
        return out
    }

    fun copy() = LinkedList(this)

    public override fun clone() = LinkedList(this)

    override fun toString(): String {
        val out = StringBuilder("[")
        var current = start
        while (current != null) {
            out.append(current.value)
            current = current.next
            if (current != null) out.append(", ")
        }
        out.append("]")
        return out.toString()
    }

    @ApiStatus.Internal
    class Node<T>(var value: T, var next: Node<T>? = null, var prev: Node<T>? = null)

    /**
     * iterator for [LinkedList].
     */
    class LLIterator<T>(private val it: LinkedList<T>, startIndex: Int) : MutableListIterator<T> {
        private var current: Node<T>? = it._get(startIndex)
        private var idx: Int = startIndex - 1
        override fun hasNext() = current != null

        override fun next(): T {
            val n = current ?: throw NoSuchElementException("no more elements")
            current = n.next
            idx++
            return n.value
        }

        fun reset(toIndex: Int = 0) {
            current = it._get(toIndex)
            idx = toIndex - 1
        }

        override fun hasPrevious() = current?.prev != null

        override fun previous(): T {
            val n = current?.prev ?: throw NoSuchElementException("no less elements")
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
            it.size++
        }

        override fun remove() {
            val n = current ?: throw NoSuchElementException("list is empty")
            if (current === it.end) it.end = n.prev
            if (current === it.start) it.start = n.next
            current = n.next
            n.prev?.next = n.next
            n.next?.prev = n.prev
            it.size--
        }

        override fun set(element: T) {
            val n = current?.prev ?: it.end ?: throw IllegalStateException("invalid, list empty?")
            n.value = element
        }
    }
}
