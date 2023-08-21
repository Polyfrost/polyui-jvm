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

import org.polyfrost.polyui.unit.Unit.*
import kotlin.math.max
import kotlin.math.min

/**
 * class to represent a unit of measurement.
 * @see Percent
 * @see Pixel
 * @see Flex
 * @see VUnits
 */
abstract class Unit(@Transient val type: Type) : Cloneable {
    /** computed pixel value of this unit. */
    abstract var px: Float

    operator fun plus(other: Unit): Float {
        return px + other.px
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
        override var px: Float = 0f
            get() {
                if (!initialized) throw UninitializedPropertyAccessException("Percent must be initialized before use.")
                return field
            }
        private var initialized: Boolean = false

        init {
            require(amount > 0f) { "Percent must be more than 0%!" }
        }

        override fun clone(): Percent {
            return Percent(amount).also { if (initialized) it.px = px }
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
            require(amount > 0f) { "Percent must be more than 0%!" }
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
            @Transient
            internal var vMin = 0f
                private set

            @Transient
            internal var vMax = 0f
                private set

            @Transient
            internal var vWidth = 0f
                set(value) = run {
                    field = value
                    vMin = min(vWidth, vHeight)
                    vMax = max(vWidth, vHeight)
                }

            @Transient
            internal var vHeight = 0f
                set(value) = run {
                    field = value
                    vMin = min(vWidth, vHeight)
                    vMax = max(vWidth, vHeight)
                }
        }
    }

    /** represents the index of a flex component */
    class Flex(val flexShrink: Int = 0, val flexGrow: Int = 0, val endRowAfter: Boolean = false) :
        Unit(Units.Flex) {
        override var px: Float = 0f

        override fun clone(): Flex {
            return Flex(flexShrink, flexGrow, endRowAfter).also {
                it.px = px
            }
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
    class Grid @JvmOverloads constructor(var row: Int, var column: Int, val rs: Int = 1, val cs: Int = 1) :
        Unit(Units.Grid) {
        override var px: Float = 0f

        override fun clone(): Grid {
            return Grid(row, column, rs, cs).also {
                it.px = px
            }
        }
    }

    /** specify a unit as an always present value that does not change. */
    interface Concrete

    /** specify a unit as something that is dependent on another value. */
    interface Dynamic {
        fun set(parent: Unit)
    }
}

typealias Units = Type
