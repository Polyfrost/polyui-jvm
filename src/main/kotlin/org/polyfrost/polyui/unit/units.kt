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

@file:JvmName("Units")
@file:Suppress("NOTHING_TO_INLINE", "UNUSED")

package org.polyfrost.polyui.unit

/** note that the smallest unit of time in PolyUI is 1 nanosecond. */
@get:JvmName("nanoseconds")
inline val Number.nanoseconds get() = toLong()

@get:JvmName("microseconds")
inline val Number.microseconds get() = (toDouble() * 1_000.0).toLong()

@get:JvmName("milliseconds")
inline val Number.milliseconds get() = (toDouble() * 1_000_000.0).toLong()

@get:JvmName("ms")
inline val Number.ms get() = (toDouble() * 1_000_000.0).toLong()

@get:JvmName("seconds")
inline val Number.seconds get() = (toDouble() * 1_000_000_000.0).toLong()

@get:JvmName("secs")
inline val Number.secs get() = (toDouble() * 1_000_000_000.0).toLong()

@get:JvmName("minutes")
inline val Number.minutes get() = (toDouble() * 60_000_000_000.0).toLong()

@get:JvmName("hours")
inline val Number.hours get() = (toDouble() * 3_600_000_000_000.0).toLong()

val AlignDefault = Align()

inline val Number.vec get() = Vec2(toFloat(), toFloat())

inline fun Float.fix() = this.toInt().toFloat()

// so much more efficient to do it this way //
inline infix fun Float.by(other: Float) = Vec2(this, other)
inline infix fun Float.by(other: Int) = Vec2(this, other.toFloat())
inline infix fun Float.by(other: Double) = Vec2(this, other.toFloat())

inline infix fun Int.by(other: Float) = Vec2(this.toFloat(), other)
inline infix fun Int.by(other: Int) = Vec2(this.toFloat(), other.toFloat())
inline infix fun Int.by(other: Double) = Vec2(this.toFloat(), other.toFloat())

inline infix fun Double.by(other: Float) = Vec2(this.toFloat(), other)
inline infix fun Double.by(other: Int) = Vec2(this.toFloat(), other.toFloat())
inline infix fun Double.by(other: Double) = Vec2(this.toFloat(), other.toFloat())


/**
 * Create a Vec4 from a Vec2, copying the [Vec2.x] and [Vec2.y] values to [Vec4.w] and [Vec4.h] respectively.
 */
fun Vec2.toVec4() = Vec4.of(x, y, x, y)
