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
@file:Suppress("UNUSED", "INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package org.polyfrost.polyui.unit

/** note that the smallest unit of time in PolyUI is 1 nanosecond. */
@get:JvmName("nanoseconds")
@kotlin.internal.InlineOnly
inline val Number.nanoseconds get() = toLong()

@get:JvmName("microseconds")
@kotlin.internal.InlineOnly
inline val Number.microseconds get() = (toDouble() * 1_000.0).toLong()

@get:JvmName("milliseconds")
@kotlin.internal.InlineOnly
inline val Number.milliseconds get() = (toDouble() * 1_000_000.0).toLong()

@get:JvmName("ms")
@kotlin.internal.InlineOnly
inline val Number.ms get() = (toDouble() * 1_000_000.0).toLong()

@get:JvmName("seconds")
@kotlin.internal.InlineOnly
inline val Number.seconds get() = (toDouble() * 1_000_000_000.0).toLong()

@get:JvmName("secs")
@kotlin.internal.InlineOnly
inline val Number.secs get() = (toDouble() * 1_000_000_000.0).toLong()

@get:JvmName("minutes")
@kotlin.internal.InlineOnly
inline val Number.minutes get() = (toDouble() * 60_000_000_000.0).toLong()

@get:JvmName("hours")
@kotlin.internal.InlineOnly
inline val Number.hours get() = (toDouble() * 3_600_000_000_000.0).toLong()

val AlignDefault = Align()

@kotlin.internal.InlineOnly
inline val Number.vec get() = Vec2(toFloat(), toFloat())

@kotlin.internal.InlineOnly
inline fun Float.fix() = this.toInt().toFloat()


// so much more efficient to do it this way //
@kotlin.internal.InlineOnly
inline infix fun Float.by(other: Float) = Vec2(this, other)

@kotlin.internal.InlineOnly
inline infix fun Float.by(other: Int) = Vec2(this, other.toFloat())

@kotlin.internal.InlineOnly
inline infix fun Float.by(other: Double) = Vec2(this, other.toFloat())


@kotlin.internal.InlineOnly
inline infix fun Int.by(other: Float) = Vec2(this.toFloat(), other)

@kotlin.internal.InlineOnly
inline infix fun Int.by(other: Int) = Vec2(this.toFloat(), other.toFloat())

@kotlin.internal.InlineOnly
inline infix fun Int.by(other: Double) = Vec2(this.toFloat(), other.toFloat())


@kotlin.internal.InlineOnly
inline infix fun Double.by(other: Float) = Vec2(this.toFloat(), other)

@kotlin.internal.InlineOnly
inline infix fun Double.by(other: Int) = Vec2(this.toFloat(), other.toFloat())

@kotlin.internal.InlineOnly
inline infix fun Double.by(other: Double) = Vec2(this.toFloat(), other.toFloat())


/**
 * Create a Vec4 from a Vec2, copying the [Vec2.x] and [Vec2.y] values to [Vec4.w] and [Vec4.h] respectively.
 */
@JvmName("toVec4")
fun Vec2.toVec4() = Vec4.of(x, y, this)
