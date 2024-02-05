package org.polyfrost.polyui.utils

import java.io.Serializable

/**
 * A mutable pair of values.
 *
 * There is no meaning attached to values in this class, it can be used for any purpose.
 * Pair exhibits value semantics, i.e. two pairs are equal if both components are equal.
 *
 * @param A type of the first value.
 * @param B type of the second value.
 * @property first First value.
 * @property second Second value.
 * @constructor Creates a new instance of Pair.
 */
data class MutablePair<A, B>(
    var first: A,
    var second: B
) : Serializable {
    /**
     * Returns string representation of the [MutablePair] including its [first] and [second] values.
     */
    override fun toString(): String = "($first, $second)"
}
