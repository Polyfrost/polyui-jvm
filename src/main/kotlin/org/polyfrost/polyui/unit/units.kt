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

@file:Suppress("NOTHING_TO_INLINE")
@file:JvmName("Units")

package org.polyfrost.polyui.unit

inline operator fun Float.compareTo(x: Unit): Int = compareTo(x.px)

@get:JvmName("pixels")
inline val Number.px get() = Unit.Pixel(this.toFloat())

@get:JvmName("percent")
inline val Number.percent get() = Unit.Percent(this.toFloat())

@get:JvmName("vwidth")
inline val Number.vwidth get() = Unit.VUnits(this.toFloat(), Unit.Type.VWidth)

@get:JvmName("vheight")
inline val Number.vheight get() = Unit.VUnits(this.toFloat(), Unit.Type.VHeight)

@get:JvmName("vmin")
inline val Number.vmin get() = Unit.VUnits(this.toFloat(), Unit.Type.VMin)

@get:JvmName("vmax")
inline val Number.vmax get() = Unit.VUnits(this.toFloat(), Unit.Type.VMax)

/** note that the smallest unit of time in PolyUI is 1 nanosecond. */
@get:JvmName("nanoseconds")
inline val Number.nanoseconds get() = toLong()

@get:JvmName("microseconds")
inline val Number.microseconds get() = (toDouble() * 1_000.0).toLong()

@get:JvmName("milliseconds")
inline val Number.milliseconds get() = (toDouble() * 1_000_000.0).toLong()

@get:JvmName("seconds")
inline val Number.seconds get() = (toDouble() * 1_000_000_000.0).toLong()

@get:JvmName("minutes")
inline val Number.minutes get() = (toDouble() * 60_000_000_000.0).toLong()

@get:JvmName("hours")
inline val Number.hours get() = (toDouble() * 3_600_000_000_000.0).toLong()

inline val origin get() = (Unit.Pixel(0f) * Unit.Pixel(0f)).clone()
inline val fill get() = (Unit.Percent(100f) * Unit.Percent(100f)).clone()

fun index(index: Int) = Unit.Flex(index)

/**
 * create a new flex unit, in vec2 form for at property.
 * @see Unit.Flex
 * @see org.polyfrost.polyui.layout.impl.FlexLayout
 */
@JvmOverloads
fun flex(flexShrink: Int = 0, flexGrow: Int = 0, endRowAfter: Boolean = false): Vec2<Unit> {
    val u = Unit.Flex(flexShrink, flexGrow, endRowAfter)
    return Vec2(u, u.clone())
}

/**
 * create a new grid unit, in vec2 form for at property.
 * @see Unit.Grid
 * @see org.polyfrost.polyui.layout.impl.GridLayout
 */
@JvmOverloads
fun grid(row: Int, column: Int, rowSpan: Int = 1, columnSpan: Int = 1): Vec2<Unit> {
    val u = Unit.Grid(row, column, rowSpan, columnSpan)
    return Vec2(u, u.clone())
}

/**
 * Declare a vec2 of unit, for either at or size property.
 *
 * example:
 * `30.px * 30.px`
 * */
inline operator fun Unit.times(other: Unit): Vec2<Unit> = Vec2(this, other)
