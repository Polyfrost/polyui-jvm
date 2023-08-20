/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/polui-jvm>
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

import kotlin.math.abs

/**
 * Class to represent a 2D vector of the given unit.
 *
 * @param T The type of unit to use
 */
data class Vec2<T : Unit>(
    /** x/width/first/a */
    val a: T,
    /** y/height/second/b */
    val b: T,
) : Cloneable {
    inline val x get() = a.px // parity with Point
    inline val y get() = b.px
    inline val width get() = a.px // parity with Size
    inline val height get() = b.px
    inline val first get() = a.px // parity with kotlin.Pair
    inline val second get() = b.px

    inline val type get() = a.type

    inline val dynamic get() = a is Unit.Dynamic || b is Unit.Dynamic

    operator fun get(index: Int): T {
        return when (index) {
            0 -> a
            1 -> b
            else -> throw IndexOutOfBoundsException()
        }
    }

    operator fun set(index: Int, value: T) {
        when (index) {
            0 -> a.px = value.px
            1 -> b.px = value.px
            else -> throw IndexOutOfBoundsException()
        }
    }

    operator fun plus(value: Vec2<T>): Vec2<T> {
        val out = clone()
        out.a.px += value.a.px
        out.b.px += value.b.px
        return out
    }

    operator fun minus(value: Vec2<T>): Vec2<T> {
        val out = clone()
        out.a.px -= value.a.px
        out.b.px -= value.b.px
        return out
    }

    operator fun times(value: Vec2<T>): Vec2<T> {
        val out = clone()
        out.a.px *= value.a.px
        out.b.px *= value.b.px
        return out
    }

    operator fun times(value: Number): Vec2<T> {
        val out = clone()
        out.a.px *= value.toFloat()
        out.b.px *= value.toFloat()
        return out
    }

    operator fun timesAssign(value: Vec2<T>) {
        this.a.px *= value.a.px
        this.b.px *= value.b.px
    }

    operator fun div(value: Vec2<T>): Vec2<T> {
        val out = clone()
        out.a.px /= value.a.px
        out.b.px /= value.b.px
        return out
    }

    operator fun div(value: Number): Vec2<T> {
        val out = clone()
        out.a.px /= value.toFloat()
        out.b.px /= value.toFloat()
        return out
    }

    operator fun divAssign(value: Vec2<T>) {
        this.a.px /= value.a.px
        this.b.px /= value.b.px
    }

    operator fun rem(value: Vec2<T>): Vec2<T> {
        val out = clone()
        out.a.px %= value.a.px
        out.b.px %= value.b.px
        return out
    }

    operator fun remAssign(value: Vec2<T>) {
        this.a.px %= value.a.px
        this.b.px %= value.b.px
    }

    operator fun unaryMinus(): Vec2<T> {
        val out = clone()
        out.a.px = -out.a.px
        out.b.px = -out.b.px
        return out
    }

    operator fun unaryPlus(): Vec2<T> {
        val out = clone()
        out.a.px = abs(out.a.px)
        out.b.px = abs(out.b.px)
        return out
    }

    operator fun plusAssign(value: Vec2<T>) {
        this.a.px += value.a.px
        this.b.px += value.b.px
    }

    operator fun minusAssign(value: Vec2<T>) {
        this.a.px -= value.a.px
        this.b.px -= value.b.px
    }

    operator fun compareTo(value: Vec2<T>): Int {
        return when {
            this.a.px > value.a.px -> 1
            this.a.px < value.a.px -> -1
            this.b.px > value.b.px -> 1
            this.b.px < value.b.px -> -1
            else -> 0
        }
    }

    fun scale(scaleX: Float, scaleY: Float) {
        this.a.px *= scaleX
        this.b.px *= scaleY
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Vec2<*>) return false
        return this.a.px == other.a.px && this.b.px == other.b.px
    }

    @Suppress("UNCHECKED_CAST")
    public override fun clone(): Vec2<T> {
        return Vec2(a.clone() as T, b.clone() as T)
    }

    override fun toString(): String {
        return "Vec2.$type(${a.px} x ${b.px})"
    }

    override fun hashCode(): Int {
        var result = a.hashCode()
        result = 31 * result + b.hashCode()
        return result
    }
}

typealias Point<T> = Vec2<T>
typealias Size<T> = Vec2<T>
