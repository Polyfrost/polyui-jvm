package cc.polyfrost.polyui.utils

/** [java.util.ArrayList.forEach] loop, that doesn't allocate any memory. Utilizes the [java.util.RandomAccess] trait.
 * @see [removeIfNoAlloc] */
inline fun <E> ArrayList<E>.forEachNoAlloc(f: (E) -> Unit) {
    for (i in 0 until this.size) {
        f(this[i])
    }
}

/** [java.util.ArrayList.removeIf], that doesn't allocate any memory. Utilizes the [java.util.RandomAccess] trait.
 * @see [forEachNoAlloc] */
inline fun <E> ArrayList<E>.removeIfNoAlloc(f: (E) -> Boolean) {
    val max = this.size
    var i = 0
    while (i < max) {
        if (f(this[i])) {
            this.removeAt(i)
        }
        i++
    }
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