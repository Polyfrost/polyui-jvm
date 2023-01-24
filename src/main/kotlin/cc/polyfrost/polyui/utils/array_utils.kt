package cc.polyfrost.polyui.utils

/** for-each this loop for array lists, that doesn't allocate any memory. Utilizes the [java.util.RandomAccess] trait. */
inline fun <E> ArrayList<E>.forEachNoAlloc(f: (E) -> Unit) {
    for (i in 0 until this.size) {
        f(this[i])
    }
}