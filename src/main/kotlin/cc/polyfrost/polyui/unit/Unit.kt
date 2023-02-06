/*
 * This file is part of PolyUI.
 * Copyright (C) 2022-2023 Polyfrost and its contributors.
 * All rights reserved.
 * PolyUI - simple, easy to use and lightweight UI framework https://polyfrost.cc https://github.com/Polyfrost/polui-jvm
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package cc.polyfrost.polyui.unit

import kotlin.math.max
import kotlin.math.min

/**
 * class to represent a unit of measurement.
 */
abstract class Unit(val type: Type) : Cloneable {
    abstract var px: Float

    operator fun plus(other: Unit): Float {
        return px + other.px
    }

    fun get(): Float {
        return px
    }

    operator fun compareTo(x: Unit): Int {
        return px.compareTo(x.px)
    }

    enum class Type {
        Pixel, Flex, Percent, VMin, VMax, VWidth, VHeight
    }

    override fun toString(): String {
        return "Unit.$type($px)"
    }

    public abstract override fun clone(): Unit

    /** pixel unit. It is just a wrapper for a float to be honest, it's that simple. */
    class Pixel(pixels: Float) : Unit(Type.Pixel), Concrete {
        override var px: Float = pixels
        override fun clone(): Pixel {
            return Pixel(px)
        }
    }

    class Percent(val amount: Float) : Unit(Type.Percent), Dynamic {
        override var px: Float = 0F
            get() {
                if (!initialized) throw UninitializedPropertyAccessException("Percent must be initialized before use.")
                return field
            }
        private var initialized: Boolean = false

        init {
            if (amount < 0 || amount > 100) throw IllegalArgumentException("Percent must be between 0 and 100 (inclusive).")
        }

        override fun clone(): Percent {
            return Percent(amount).also { it.px = px }
        }

        override fun set(parent: Unit) {
            initialized = true
            px = parent.px * (amount / 100f)
        }
    }


    class VUnits(val amount: Float, type: Type) : Unit(type), Dynamic {
        override var px: Float = 0f

        init {
            if (amount < 0 || amount > 100) throw IllegalArgumentException("Percent must be between 0 and 100 (inclusive).")
        }

        override fun clone(): VUnits {
            return VUnits(amount, type).also { it.px = px }
        }

        override fun set(parent: Unit) {
            px = when (type) {
                Type.VWidth -> vWidth
                Type.VHeight -> vHeight
                Type.VMin -> vMin
                Type.VMax -> vMax
                else -> throw IllegalArgumentException("VUnits must be either VWidth, VHeight, VMin, or VMax.")
            } * (amount / 100f)
        }

        companion object {
            internal var vMin = 0f
                private set
            internal var vMax = 0f
                private set
            internal var vWidth = 0f
                set(value) = run { field = value; vMin = min(vWidth, vHeight); vMax = max(vWidth, vHeight) }
            internal var vHeight = 0f
                set(value) = run { field = value; vMin = min(vWidth, vHeight); vMax = max(vWidth, vHeight) }
        }
    }


    /** represents the index of a flex component */
    class Flex(val index: Int = -1, val flexShrink: Int = 0, val flexGrow: Int = 0, val endRowAfter: Boolean = false) :
        Unit(Units.Flex) {
        override var px: Float = 0f

        override fun clone(): Flex {
            return Flex(index, flexShrink, flexGrow, endRowAfter)
        }
    }


    /** specify a unit as an always present value that does not change. */
    interface Concrete {
        fun clone(): Concrete
    }

    /** specify a unit as something that is dependent on another value. */
    interface Dynamic {
        fun clone(): Dynamic

        fun set(parent: Unit)
    }
}

typealias Units = Unit.Type




