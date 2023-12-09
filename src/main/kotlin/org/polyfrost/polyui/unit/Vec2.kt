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

package org.polyfrost.polyui.unit

import org.jetbrains.annotations.ApiStatus
import kotlin.math.sqrt

@Suppress("invisible_member", "invisible_reference")
open class Vec2(open var x: Float = 0f, open var y: Float = 0f) : Comparable<Vec2> {
    @kotlin.internal.InlineOnly
    inline var width
        get() = x
        set(value) {
            x = value
        }

    @kotlin.internal.InlineOnly
    inline var height
        get() = y
        set(value) {
            y = value
        }

    @kotlin.internal.InlineOnly
    inline val isZero get() = x == 0f && y == 0f

    @kotlin.internal.InlineOnly
    inline val hasZero get() = x == 0f || y == 0f

    @kotlin.internal.InlineOnly
    inline val isNegative get() = x < 0f && y < 0f

    /**
     * Magnitude² (squared) of the vector
     * @see magnitude
     */
    open val magnitude2 get() = (x * x) + (y * y)

    /**
     * Magnitude of the vector.
     * @see magnitude2
     */
    open val magnitude get() = sqrt(magnitude2)

    /**
     * Normalize this vector.
     */
    @ApiStatus.Experimental
    fun normalize() {
        val mag = magnitude
        x /= mag
        y /= mag
    }

    fun min(x: Float, y: Float, respectRatio: Boolean = true) {
        if (this.x < x || this.y < y) {
            resize(x, y, respectRatio)
        }
    }

    fun max(x: Float, y: Float, respectRatio: Boolean = true) {
        if (this.x > x || this.y > y) {
            resize(x, y, respectRatio)
        }
    }

    fun resize(x: Float, y: Float, respectRatio: Boolean = true) {
        if (respectRatio) {
            val ratio = this.x / this.y
            if (x / y > ratio) {
                this.x = y * ratio
                this.y = y
            } else {
                this.x = x
                this.y = x / ratio
            }
        } else {
            this.x = x
            this.y = y
        }
    }

    fun scale(xScale: Float, yScale: Float) {
        x *= xScale
        y *= yScale
    }

    operator fun timesAssign(other: Vec2?) {
        if (other == null) return
        this.x *= other.x
        this.y *= other.y
    }

    operator fun timesAssign(other: Float) {
        this.x *= other
        this.y *= other
    }

    operator fun plus(vec2: Vec2): Vec2 {
        return Vec2(this.x + vec2.x, this.y + vec2.y)
    }

    operator fun plusAssign(other: Vec2?) {
        if (other == null) return
        this.x += other.x
        this.y += other.y
    }

    operator fun minusAssign(other: Vec2?) {
        if (other == null) return
        this.x -= other.x
        this.y -= other.y
    }

    operator fun set(index: Int, value: Float) {
        when (index) {
            0 -> this.x = value
            1 -> this.y = value
            else -> throw IndexOutOfBoundsException(index.toString())
        }
    }

    fun set(other: Vec2) {
        this.x = other.x
        this.y = other.y
    }

    operator fun get(index: Int): Float {
        return when (index) {
            0 -> this.x
            1 -> this.y
            else -> throw IndexOutOfBoundsException(index.toString())
        }
    }

    /**
     * Compares the two Vec2 instances according to the magnitude of the vectors.
     * A positive number is returned if this magnitude is larger than [other], negative if it is smaller than [other], and 0 if it is equal to [other].
     *
     * Note that two different vectors can have the same magnitude.
     * @see [Float.compareTo]
     */
    override operator fun compareTo(other: Vec2) = this.magnitude2.compareTo(other.magnitude2)

    /**
     * Compares the magnitude of this to the given float, which should be a magnitude of a vector itself.
     * @see compareTo
     */
    operator fun compareTo(fl: Float) = magnitude.compareTo(fl)

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is Vec2) return false
        return this.x == other.x && this.y == other.y
    }

    override fun hashCode(): Int {
        var res = x.hashCode()
        res = 31 * res + y.hashCode()
        return res
    }

    override fun toString() = "${x}x$y"

    abstract class Sourced(source: Vec2? = null) : Vec2() {
        @get:Deprecated("external access to the source value is bad practice.")
        var source: Vec2? = source
            set(value) {
                if (value == null) return
                if (value == field) return
                sourceChanged(value)
                field = value
            }

        @get:Suppress("Deprecation")
        inline val sourced get() = source != null

        protected open fun sourceChanged(source: Vec2) {}

        protected fun source(): Vec2 {
            @Suppress("Deprecation")
            return source ?: throw UninitializedPropertyAccessException("source vector not initialized")
        }

        protected fun source(or: Vec2): Vec2 {
            @Suppress("Deprecation")
            return source ?: or
        }
    }

    class Relative(x: Float = 0f, y: Float = 0f, parent: Vec2? = null) : Sourced(parent) {
        override var x: Float
            get() = super.x + source(or = ZERO).x
            set(value) {
                super.x = value - source(or = ZERO).x
            }
        override var y: Float
            get() = super.y + source(or = ZERO).y
            set(value) {
                super.y = value - source(or = ZERO).y
            }

        /**
         * Effectively make this vector equal to the [source].
         */
        fun zero() {
            x -= source(or = ZERO).x
            y -= source(or = ZERO).y
        }

        init {
            this.x = x
            this.y = y
        }
    }

    // Based.
    class Based(x: Float = 0f, y: Float = 0f, base: Vec2? = null) : Sourced(base) {
        override var x: Float
            get() = super.x + source(or = ZERO).x
            set(value) {
                super.x = value
            }

        override var y: Float
            get() = super.y + source(or = ZERO).y
            set(value) {
                super.y = value
            }

        init {
            this.x = x
            this.y = y
        }
    }

    class Percent(percentX: Float, percentY: Float, source: Vec2? = null) : Sourced(source) {
        override var x: Float
            get() = super.x * source(or = ZERO).x
        override var y: Float
            get() = super.y * source(or = ZERO).y

        init {
            this.x = percentX
            this.y = percentY
        }
    }

    /**
     * Immutable Vec2 wrapper.
     * Attempts to set will result in a compilation [error][DeprecationLevel.ERROR] in Kotlin, and an [UnsupportedOperationException].
     */
    class Immutable(x: Float, y: Float) : Vec2(x, y) {
        @set:Deprecated("Cannot set immutable Vec2", level = DeprecationLevel.ERROR)
        override var x: Float
            get() = super.x
            set(_) {
                throw UnsupportedOperationException("vec2 is immutable")
            }

        @set:Deprecated("Cannot set immutable Vec2", level = DeprecationLevel.ERROR)
        override var y: Float
            get() = super.y
            set(_) {
                throw UnsupportedOperationException("vec2 is immutable")
            }

        override val magnitude2 = x * x + y * y
        override val magnitude = sqrt(magnitude2)
    }

    companion object {
        @JvmField
        val ONE = Immutable(1f, 1f)

        @JvmField
        val ZERO = Immutable(0f, 0f)
    }
}
