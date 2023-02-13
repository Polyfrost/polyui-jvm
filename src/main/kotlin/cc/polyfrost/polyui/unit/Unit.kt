/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors. All rights reserved.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/polui-jvm>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package cc.polyfrost.polyui.unit

import cc.polyfrost.polyui.unit.Unit.*
import kotlin.math.max
import kotlin.math.min

/**
 * class to represent a unit of measurement.
 * @see Percent
 * @see Pixel
 * @see Flex
 * @see VUnits
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
        Pixel, Flex, Grid, Percent, VMin, VMax, VWidth, VHeight
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

    /** a dynamic unit, that is a percentage of its parents size. */
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

    /** viewport-specific units. */
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

    /**
     * Represents a grid component.
     *
     * @param row the row of the grid to place the component in.
     * @param column the column of the grid to place the component in.
     * @param rs the number of rows the component should span.
     * @param cs the number of columns the component should span.
     *
     * @since 0.10.0
     */
    class Grid @JvmOverloads constructor(val row: Int, val column: Int, val rs: Int = 1, val cs: Int = 1) :
        Unit(Units.Grid) {
        override var px: Float = 0f

        override fun clone(): Grid {
            return Grid(row, column, rs, cs)
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

typealias Units = Type
