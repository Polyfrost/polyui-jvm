/** This file contains various utilities for arrays and arraylists. */
@file:Suppress("ReplaceManualRangeWithIndicesCalls", "ReplaceSizeZeroCheckWithIsEmpty")

package cc.polyfrost.polyui.utils

/**
 * [java.util.List.forEach] re-implementation that doesn't allocate any memory.
 *
 * Utilizes the [java.util.RandomAccess] trait.
 *
 * @param f The function to apply to each element.
 *
 * @see [fastRemoveIf]
 */
inline fun <L, E> L.fastEach(f: (E) -> Unit) where L : List<E>, L : RandomAccess {
    if (this.size == 0) return
    for (i in 0 until this.size) {
        f(this[i])
    }
}

/**
 * [java.util.List.forEachIndexed] re-implementation that doesn't allocate any memory.
 *
 * Utilizes the [java.util.RandomAccess] trait.
 *
 * @param f The function to apply to each element.
 *
 * @see [fastRemoveIf]
 */
inline fun <L, E> L.fastEachIndexed(f: (Int, E) -> Unit) where L : List<E>, L : RandomAccess {
    if (this.size == 0) return
    for (i in 0 until this.size) {
        f(i, this[i])
    }
}

/**
 * [java.util.List.removeIf] re-implementation that doesn't allocate any memory.
 *
 * Utilizes the [java.util.RandomAccess] trait.
 *
 * @see [fastEach]
 */
inline fun <L, E> L.fastRemoveIf(f: (E) -> Boolean) where L : MutableList<E>, L : RandomAccess {
    if (this.size == 0) return
    var max = this.size
    var i = 0
    while (i < max) {
        if (f(this[i])) {
            this.removeAt(i)
            max--
        }
        i++
    }
}

/**
 * Returns a list of all elements sorted descending according to natural sort order of the value returned by specified selector function.
 *
 * The sort is *stable*. It means that equal elements preserve their order relative to each other after sorting.
 */
inline fun <L, reified E, R : Comparable<R>> L.sortedByDescending(crossinline selector: (E) -> R?): L where L : MutableList<E>, L : RandomAccess {
    if (this.size <= 1) return this
    return this.toTypedArray().sortedByDescending(selector).toMutableList() as L
}


/**
 * Returns the sum of all values produced by [selector] function applied to
 * each element in the collection, for floats.
 *
 * @param selector a function that extracts a [Float] property of an element
 * @return the sum of all values produced by [selector]
 *
 * @see [fastEach]
 */
inline fun <L, E> L.sumOf(selector: (E) -> Float): Float where L : List<E>, L : RandomAccess {
    if (this.size == 0) return 0f
    var sum = .0f
    fastEach {
        sum += selector(it)
    }
    return sum
}

/**
 * Moves the given element from the [from] index to the [to] index.
 *
 * **Note**: this method makes absolutely no attempt to verify if the given
 * indices are valid.
 *
 * @param from the index of the element to move
 * @param to the index to move the element to
 */
fun <E> Array<E>.moveElement(from: Int, to: Int) {
    val item = this[from]
    this[from] = this[to]
    this[to] = item
}

/**
 * Append [element] to the end of this array, that is, the first empty index.
 *
 * @param element the element to append
 * @param stillPutOnFail set this to `true` if you want it to [add][Array.plus]
 *                       the element (causes a reallocation!) even if the array
 *                       is full. (default: `false`)
 * @throws IndexOutOfBoundsException if the array is full and [stillPutOnFail]
 *                                   is set to `false`.
 */
fun <E> Array<E>.append(element: E, stillPutOnFail: Boolean = false): Array<E> {
    forEachIndexed { i, it ->
        if (it == null) {
            this[i] = element
            return this
        }
    }
    if (stillPutOnFail) {
        return this.plus(element)
    } else throw IndexOutOfBoundsException("Array is already full!")
}

fun <T> Iterable<T>.toArrayList(): ArrayList<T> {
    return if (this is ArrayList) this
    else this.toMutableList() as ArrayList<T>
}

fun <T> Array<T>.toArrayList(): ArrayList<T> = this.toMutableList() as ArrayList<T>