@file:Suppress("EqualsOrHashCode")

package org.polyfrost.polyui.unit

import kotlin.math.sqrt

/**
 * Class that represents a 4D vector.
 * @since 1.5.0
 */
abstract class Vec4 : Comparable<Vec4>, Cloneable {
    abstract val x: Float
    abstract val y: Float
    abstract val w: Float
    abstract val h: Float

    open val isNegative: Boolean
        get() = x < 0f || y < 0f || w < 0f || h < 0f

    open val magnitude2 get() = x * x + y * y + w * w + h * h

    val magnitude get() = sqrt(magnitude2)

    fun compareTo(other: Float) = magnitude2.compareTo(other * other)

    override fun compareTo(other: Vec4) = magnitude2.compareTo(other.magnitude2)

    open operator fun get(index: Int) = when (index) {
        0 -> x
        1 -> y
        2 -> w
        3 -> h
        else -> throw IndexOutOfBoundsException("Index: $index")
    }

    operator fun component1() = x
    operator fun component2() = y
    operator fun component3() = w
    operator fun component4() = h

    public abstract override fun clone(): Vec4

    open fun copy(x: Float = this.x, y: Float = this.y, w: Float = this.w, h: Float = this.h) = of(x, y, w, h)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Vec4) return false

        if (x != other.x) return false
        if (y != other.y) return false
        if (w != other.w) return false
        if (h != other.h) return false

        return true
    }

    override fun toString() = "${x}x${y}x${w}x$h"

    private class Impl(private val xy: Vec2, private val wh: Vec2) : Vec4() {
        override val x get() = xy.x
        override val y get() = xy.y
        override val w get() = wh.x
        override val h get() = wh.y
        override fun clone() = Impl(xy, wh)

        override val isNegative: Boolean
            get() = xy.isNegative && wh.isNegative

        override val magnitude2: Float
            get() = xy.magnitude2 + wh.magnitude2
    }

    private class Single(override val x: Float) : Vec4() {
        override val y get() = x
        override val w get() = x
        override val h get() = x

        override val isNegative: Boolean
            get() = x < 0f

        override val magnitude2 get() = (x * x) * 4f

        override fun clone() = Single(x)

        override fun get(index: Int) = x

        override fun toString() = "$x"
    }


    companion object Constants {
        @JvmField
        val ONE: Vec4 = Single(1f)

        @JvmField
        val ZERO: Vec4 = Single(0f)

        @JvmStatic
        fun of(x: Float, y: Float, w: Float, h: Float): Vec4 = Impl(Vec2(x, y), Vec2(w, h))

        @JvmStatic
        @JvmName("of")
        fun of(x: Float, y: Float, dims: Vec2): Vec4 = Impl(Vec2(x, y), dims)

        @JvmStatic
        @JvmName("of")
        fun of(at: Vec2, size: Vec2): Vec4 = Impl(at, size)
    }
}
