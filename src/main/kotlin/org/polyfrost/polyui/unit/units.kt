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

@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@file:JvmName("Units")

package org.polyfrost.polyui.unit

import kotlin.jvm.internal.Ref.LongRef

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

@get:JvmName("seconds")
@kotlin.internal.InlineOnly
inline val Number.seconds get() = (toDouble() * 1_000_000_000.0).toLong()

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
inline fun Long.toChromaSpeed() = LongRef().also { it.element = this }

fun Vec2.makeRelative(to: Vec2? = null): Vec2.Relative {
    return if (this is Vec2.Relative) {
        this.source = to
        this
    } else if (to == null) {
        Vec2.Relative(this.x, this.y)
    } else {
        Vec2.Relative(this.x - to.x, this.y - to.y, to)
    }

}

/**
 * A negative vec2 which is used to indicate that the given drawable with this property should be ignored.
 * @see org.polyfrost.polyui.component.Positioner.Default
 */
@kotlin.internal.InlineOnly
inline val ignored: Vec2 get() = Vec2(-0.001f, -0.001f)

@kotlin.internal.InlineOnly
inline fun percent(percentX: Float, percentY: Float): Vec2 = Vec2.Percent(percentX, percentY)

@kotlin.internal.InlineOnly
inline infix fun Number.by(other: Number): Vec2 = Vec2(this.toFloat(), other.toFloat())
