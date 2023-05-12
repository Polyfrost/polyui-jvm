/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors. All rights reserved.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/polui-jvm>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

@file:Suppress("NOTHING_TO_INLINE")
@file:JvmName("Units")

package cc.polyfrost.polyui.unit

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

@get:JvmName("nanoseconds")
inline val Number.nanoseconds get() = toLong()

@get:JvmName("milliseconds")
inline val Number.milliseconds get() = (toLong() * 1_000_000L)

@get:JvmName("seconds")
inline val Number.seconds get() = (toLong() * 1_000_000_000L)

@get:JvmName("minutes")
inline val Number.minutes get() = (toLong() * 60_000_000_000L)

@get:JvmName("hours")
val Number.hours get() = (toDouble() * 3_600_000.0).toLong()

inline val origin get() = (0.px * 0.px).clone()
inline val fill get() = Unit.Percent(100f).clone()
inline val fillv get() = (Unit.Percent(100f) * Unit.Percent(100f)).clone()

fun index(index: Int) = Unit.Flex(index)

/**
 * create a new flex unit, in vec2 form for at property.
 * @see Unit.Flex
 * @see cc.polyfrost.polyui.layout.impl.FlexLayout
 */
@JvmOverloads
fun flex(index: Int = -1, flexShrink: Int = 0, flexGrow: Int = 0, endRowAfter: Boolean = false): Vec2<Unit> {
    val u = Unit.Flex(index, flexShrink, flexGrow, endRowAfter)
    return Vec2(u, u.clone())
}

/**
 * create a new grid unit, in vec2 form for at property.
 * @see Unit.Grid
 * @see cc.polyfrost.polyui.layout.impl.GridLayout
 */
@JvmOverloads
fun grid(row: Int, column: Int, rowSpan: Int = 1, columnSpan: Int = 1): Vec2<Unit> {
    val u = Unit.Grid(row, column, rowSpan, columnSpan)
    return Vec2(u, u.clone())
}

/**
 * Declare a vec2 of unit, for either at or sized property.
 *
 * example:
 * `30.px * 30.px`
 * */
inline operator fun Unit.times(other: Unit): Vec2<Unit> = Vec2(this, other)
