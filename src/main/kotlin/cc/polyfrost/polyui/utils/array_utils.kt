/** This file contains various utilities for arrays and arraylists. */
package cc.polyfrost.polyui.utils

/** [java.util.ArrayList.forEach] loop, that doesn't allocate any memory. Utilizes the [java.util.RandomAccess] trait.
 * @see [removeIfNoAlloc] */
inline fun <E> ArrayList<out E>.forEachNoAlloc(f: (E) -> Unit) {
    if (this.size == 0) return
    for (i in 0 until this.size) {
        f(this[i])
    }
}

inline fun <E> ArrayList<out E>.forEachIndexedNoAlloc(f: (Int, E) -> Unit) {
    if (this.size == 0) return
    for (i in 0 until this.size) {
        f(i, this[i])
    }
}

/** [java.util.ArrayList.removeIf], that doesn't allocate any memory. Utilizes the [java.util.RandomAccess] trait.
 * @see [forEachNoAlloc] */
inline fun <E> ArrayList<E>.removeIfNoAlloc(f: (E) -> Boolean) {
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
 * Returns the sum of all values produced by [selector] function applied to each element in the collection, for floats.
 *
 * Also utilizes [forEachNoAlloc] to avoid memory allocation.
 */
inline fun <E> ArrayList<E>.sumOf(selector: (E) -> Float): Float {
    var sum = 0F
    this.forEachNoAlloc {
        sum += selector(it)
    }
    return sum
}

/** moves the given element from the [from] index to the [to] index.
 *
 * **note:** this method makes absolutely no attempt to verify if the given indices is valid. */
inline fun <E> Array<E>.moveElement(from: Int, to: Int) {
    val item = this[from]
    this[from] = this[to]
    this[to] = item
}

/** append [element] to the end of this array, that is, the first empty index.
 *
 * @param stillPutOnFail set this to true if you want it to [add][Array.plus] the element (causes a reallocation!) even if the array is full.
 * @throws IndexOutOfBoundsException if the array is full and [stillPutOnFail] is false.
 */
inline fun <E> Array<E>.append(element: E, stillPutOnFail: Boolean = false): Array<E> {
    this.forEachIndexed { i, it ->
        if (it == null) {
            this[i] = element
            return this
        }
    }
    if (stillPutOnFail) {
        return this.plus(element)
    } else throw IndexOutOfBoundsException("Array is already full!")
}