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
 *
 * *(revised 1.5)* Vec2's are now purely syntactical sugar which are inlined and have no runtime overhead. Their representation in internal data structures
 * has been replaced with floats instead.
 * @since 1.1.0
 */
@JvmInline
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
value class Vec2 private constructor(@PublishedApi internal val value: Long) {
    constructor(x: Float, y: Float) : this(x.toBits().toLong() or y.toBits().toLong().shl(32))
    // rewrite counter: 5

    @kotlin.internal.InlineOnly
    inline val x: Float get() = java.lang.Float.intBitsToFloat(value.toInt())

    @kotlin.internal.InlineOnly
    inline val y: Float get() = java.lang.Float.intBitsToFloat(value.shr(32).toInt())

    // checks if the value is negative by checking the sign bits in one go
//    @kotlin.internal.InlineOnly
    val isNegative get() = (value and ((1L shl 63) or (1L shl 31))) != 0L

    @kotlin.internal.InlineOnly
    inline val isZero get() = value == 0L

//    @kotlin.internal.InlineOnly
    val isPositive get() = !isNegative && value != 0L

    operator fun get(index: Int) = when (index) {
        0 -> x
        1 -> y
        else -> throw IndexOutOfBoundsException("Index: $index")
    }

    operator fun component1() = x
    operator fun component2() = y

    override fun toString() = "${x}x$y"

    companion object Constants {
        @get:JvmName("ZERO")
        val ZERO = Vec2(0L)

        // dw bout it :smile:
        @get:JvmName("ONE")
        val ONE = Vec2(0x3f8000003f800000L)
    }
}
