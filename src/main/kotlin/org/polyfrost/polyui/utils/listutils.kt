@file:JvmName("ListUtils")
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package org.polyfrost.polyui.utils

import org.jetbrains.annotations.ApiStatus
import org.polyfrost.polyui.PolyUI

/**
 * no-alloc [Iterable.forEach] which utilizes [RandomAccess] to get the elements.
 *
 * **note:** this method contains a simple check for [ConcurrentModificationException].
 * if concurrent modification is detected, **it will not throw an exception** and simply print `FAST_WARN_CONCURRENT_MODIFICATION`
 * to the console. **This method is not thread safe.**
 * @since 1.2.0
 * @see fastRemoveIfReversed
 * @see fastEachReversed
 */
@kotlin.internal.InlineOnly
inline fun <L, E> L.fastEach(func: (E) -> Unit) where L : List<E>, L : RandomAccess {
    for (i in indices) {
        // i can already hear the JIT crying in de-optimization: trap hit: unstable_if
        if (i > this.size - 1) {
            PolyUI.LOGGER.error("FAST_WARN_CONCURRENT_MODIFICATION")
            return
        }
        func(this[i])
    }
}

/**
 * no-alloc [Iterable.forEachIndexed] which utilizes [RandomAccess] to get the elements.
 *
 * **note:** this method contains a simple check for [ConcurrentModificationException].
 * if concurrent modification is detected, **it will not throw an exception** and simply print `WARN_CONCURRENT_MODIFICATION_IDX`
 * to the console. **This method is not thread safe.**
 * @since 1.2.0
 * @see fastRemoveIfReversed
 * @see fastEachReversed
 */
@kotlin.internal.InlineOnly
inline fun <L, E> L.fastEachIndexed(func: (Int, E) -> Unit) where L : List<E>, L : RandomAccess {
    for (i in indices) {
        if (i > this.size - 1) {
            PolyUI.LOGGER.error("FAST_WARN_CONCURRENT_MODIFICATION_IDX")
            return
        }
        func(i, this[i])
    }
}

/**
 * no-alloc [List.forEach] which utilizes [RandomAccess] to get the elements, backwards.
 *
 * **note:** this method contains a simple check for [ConcurrentModificationException].
 * if concurrent modification is detected, **it will not throw an exception** and simply print `WARN_CONCURRENT_MODIFICATION_REV`
 * to the console. **This method is not thread safe.**
 * @since 1.2.0
 * @see fastRemoveIfReversed
 * @see fastEach
 */
@kotlin.internal.InlineOnly
inline fun <L, E> L.fastEachReversed(func: (E) -> Unit) where L : List<E>, L : RandomAccess {
    for (i in indices.reversed()) {
        if (i > this.size - 1) {
            PolyUI.LOGGER.error("FAST_WARN_CONCURRENT_MODIFICATION_REV")
            return
        }
        func(this[i])
    }
}


/**
 * no-alloc [MutableList.removeIf] which utilizes [RandomAccess] to get the elements.
 *
 * **This method is not thread safe.** if the list is modified while this loop is running by an external source,
 * the behavior is undefined.
 *
 * @since 1.2.0
 * @see fastEach
 * @see fastRemoveIfReversed
 */
@Deprecated("This method is less efficient than its reverse counterpart, which should be used where possible.", ReplaceWith("fastRemoveIfReversed"))
@kotlin.internal.InlineOnly
inline fun <L, E> L.fastRemoveIf(predicate: (E) -> Boolean) where L : MutableList<E>, L : RandomAccess {
    var i = 0
    while (i < this.size) {
        if (predicate(this[i])) {
            this.removeAt(i)
        } else i++
    }
}

/**
 * no-alloc [MutableList.removeIf] which utilizes [RandomAccess] to get the elements.
 *
 * **This method is not thread safe.** if the list is modified while this loop is running by an external source,
 * the behavior is undefined.
 *
 * this method should be used in preference to [fastRemoveIf] as it is more efficient with the removal operations.
 *
 * @since 1.2.0
 * @see fastEach
 */
@kotlin.internal.InlineOnly
inline fun <L, E> L.fastRemoveIfReversed(predicate: (E) -> Boolean) where L : MutableList<E>, L : RandomAccess {
    for (i in indices.reversed()) {
        if (i > this.size - 1) {
            PolyUI.LOGGER.error("FAST_WARN_CONCURRENT_MODIFICATION_RM_REV")
            return
        }
        if (predicate(this[i])) {
            this.removeAt(i)
        }
    }
}

/**
 * Returns `true` if **all elements** match the given [predicate].
 *
 * **Note this function** will return `true` if the list is empty, according to the principle of [Vacuous truth](https://en.wikipedia.org/wiki/Vacuous_truth),
 * and the fact that there are no elements that don't match the given predicate.
 *
 * **This method is not thread safe**. This function *may* fail with [ConcurrentModificationException] in case of illegal access between threads.
 *
 * *this method is no-alloc, like other methods in this family such as [fastEach].*
 * @since 1.2.0
 */
@kotlin.internal.InlineOnly
inline fun <L, E> L.fastAll(predicate: (E) -> Boolean): Boolean where L : List<E>, L : RandomAccess {
    for (i in indices) {
        if (i > this.size - 1) throw ConcurrentModificationException()
        if (!predicate(this[i])) {
            return false
        }
    }
    return true
}

/**
 * Returns `true` if **all elements** match the given [predicate].
 *
 * **Note this function** will return `true` if the list is empty, according to the principle of [Vacuous truth](https://en.wikipedia.org/wiki/Vacuous_truth),
 * and the fact that there are no elements that don't match the given predicate.
 *
 * **This method is not thread safe.** This function *may* fail with [ConcurrentModificationException] in case of illegal access between threads.
 *
 * *this method is no-alloc, like other methods in this family such as [fastEach].*
 * @since 1.2.0
 */
@kotlin.internal.InlineOnly
inline fun <L, E> L.fastAny(predicate: (E) -> Boolean): Boolean where L : List<E>, L : RandomAccess {
    for (i in indices) {
        if (i > this.size - 1) throw ConcurrentModificationException()
        if (predicate(this[i])) {
            return true
        }
    }
    return false
}

/**
 * Perform the given [transform] on every element in this list, and return a new array with the results.
 *
 * Equivalent to `this.map { transform(it) }.toTypedArray()`, but saves on the creation of an intermediate list.
 *
 * *this method is no-alloc, like other methods in this family such as [fastEach].*
 */
@kotlin.internal.InlineOnly
inline fun <L, E, reified R> L.mapToArray(transform: (E) -> R): Array<R> where L : List<E>, L : RandomAccess {
    val out = arrayOfNulls<R>(size)
    this.fastEachIndexed { i, it ->
        out[i] = transform(it)
    }
    @Suppress("UNCHECKED_CAST")
    return out as Array<R>
}

/**
 * Add the given [element] to this list if it is not in it already.
 *
 * Used when the performance hit of a [Set] is not worth it, such as if this operation is rare.
 * @since 1.2.0
 */
fun <E> MutableList<E>.addIfAbsent(element: E) {
    if (!contains(element)) this.add(element)
}

/**
 * Replace this given element if it already exists in this list using [Any.equals], or append it to the
 * end if it does not.
 * This method is very strange and has one specific use case, which is for cases where
 * *referential equality* is needed as well as *structural equality*. See more [here](https://kotlinlang.org/docs/equality.html).
 * @since 1.2.0
 * @see addIfAbsent
 */
@ApiStatus.Internal
fun <E> MutableList<E>.addOrReplace(element: E): E? {
    val i = indexOf(element)
    return if (i != -1) {
        val old = this[i]
        this[i] = element
        old
    } else {
        this.add(element)
        null
    }
}

/**
 * Returns this list, with all elements not in the range [from]..[to] removed.
 *
 * Functionally equivalent to [MutableList.subList] but effects this list, not returning a copy with the specified elements.
 *
 * *this method is no-alloc, like other methods in this family such as [fastEach].*
 *
 * **This method is not thread safe.** If the original list is being iterated over when this method is called,
 * it will probably blow up.
 * @since 1.2.0
 * @see fastEach
 */
fun <L, E> L.cut(from: Int, to: Int): L where L : MutableList<E>, L : RandomAccess {
    require(from <= to) { "from must be less than or equal to to ($from..$to)" }
    require(from >= 0 && to < this.size) { "from and to must be within the bounds of the list (got $from..$to, expected 0..${this.size - 1})" }

    for (i in this.size - 1 downTo to + 1) {
        this.removeAt(i)
    }

    for (i in from - 1 downTo 0) {
        this.removeAt(i)
    }

    return this
}
