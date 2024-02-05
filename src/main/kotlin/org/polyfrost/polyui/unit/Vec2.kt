/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023-2024 Polyfrost and its contributors.
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

/**
 * Class that represents a 2D vector.
 * @since 1.1.0
 */
open class Vec2(open val x: Float, open val y: Float) : Cloneable {
    constructor() : this(0f, 0f)

    // rewrite counter: 3

    operator fun get(index: Int): Float = when (index) {
        0 -> x
        1 -> y
        else -> throw IndexOutOfBoundsException("Index: $index")
    }

    operator fun plus(other: Vec2) = Vec2(x + other.x, y + other.y)

    operator fun minus(other: Vec2) = Vec2(x - other.x, y - other.y)

    operator fun times(other: Vec2) = Vec2(x * other.x, y * other.y)

    operator fun div(other: Vec2) = Vec2(x / other.x, y / other.y)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Vec2) return false

        return this.x == other.x && this.y == other.y
    }

    override fun hashCode(): Int {
        var result = x.hashCode()
        result = 31 * result + y.hashCode()
        return result
    }

    public override fun clone(): Vec2 {
        return super.clone() as Vec2
    }

    override fun toString() = "${x}x$y"

    /**
     * Mutable extension of [Vec2].
     */
    open class Mut(override var x: Float, override var y: Float) : Vec2(x, y) {
        constructor() : this(0f, 0f)

        operator fun set(index: Int, value: Float) {
            when (index) {
                0 -> x = value
                1 -> y = value
                else -> throw IndexOutOfBoundsException("Index: $index")
            }
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

        open fun scale(xScale: Float, yScale: Float) {
            x *= xScale
            y *= yScale
        }

        operator fun timesAssign(other: Vec2?) {
            if (other == null) return
            scale(other.x, other.y)
        }

        operator fun timesAssign(other: Float) = scale(other, other)

        operator fun divAssign(other: Float) = scale(1f / other, 1f / other)

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

        fun set(other: Vec2) {
            this.x = other.x
            this.y = other.y
        }

        /**
         * simple [min] function.
         */
        fun smin(other: Vec2) {
            this.x = kotlin.math.min(other.x, this.x)
            this.y = kotlin.math.min(other.y, this.y)
        }

        /**
         * simple [max] function.
         */
        fun smax(other: Vec2) {
            this.x = kotlin.math.max(other.x, this.x)
            this.y = kotlin.math.max(other.y, this.y)
        }
    }

    companion object Constants {
        @JvmField
        val ONE = Vec2(1f, 1f)

        @JvmField
        val ZERO = Vec2(0f, 0f)

        @JvmField
        val M1 = Vec2(-1f, -1f)

        @JvmField
        val RES_1080P = Vec2(1920f, 1080f)

        @JvmField
        val RES_1440P = Vec2(2560f, 1440f)

        @JvmField
        val RES_4K = Vec2(3840f, 2160f)
    }
}
