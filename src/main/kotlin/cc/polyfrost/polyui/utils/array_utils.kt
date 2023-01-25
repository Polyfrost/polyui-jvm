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