/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors. All rights reserved.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/polui-jvm>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package cc.polyfrost.polyui.unit

/**
 * Class to represent a 2D vector of the given unit.
 *
 * @param T The type of unit to use
 */
data class Vec2<T : Unit>(
    val a: T,
    val b: T
) : Cloneable {
    inline val x get() = a.px
    inline val y get() = b.px
    inline val width get() = a.px
    inline val height get() = b.px

    inline val type get() = a.type

    operator fun get(index: Int): T {
        return when (index) {
            0 -> a
            1 -> b
            else -> throw IndexOutOfBoundsException()
        }
    }

    operator fun plus(value: Vec2<T>): Vec2<T> {
        this.a.px + value.a.px
        this.b.px + value.b.px
        return this
    }

    operator fun minus(value: Vec2<T>): Vec2<T> {
        this.a.px - value.a.px
        this.b.px - value.b.px
        return this
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

    fun move(x: Float, y: Float): Vec2<T> {
        this.a.px += x
        this.b.px += y
        return this
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Vec2<*>) return false
        if (this === other) return true
        return this.a.px == other.a.px && this.b.px == other.b.px
    }

    @Suppress("UNCHECKED_CAST")
    public override fun clone(): Vec2<T> {
        return Vec2(a.clone() as T, b.clone() as T)
    }

    override fun toString(): String {
        return "Vec2.$type(${a.px} x ${b.px})"
    }

    fun scale(scaleX: Float, scaleY: Float) {
        this.a.px *= scaleX
        this.b.px *= scaleY
    }
}

typealias Point<T> = Vec2<T>
typealias Size<T> = Vec2<T>
