package cc.polyfrost.polyui.unit

import kotlin.properties.Delegates

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
        override var px: Float by Delegates.notNull()

        init {
            if (amount < 0 || amount > 100) throw IllegalArgumentException("Percent must be between 0 and 100 (inclusive).")
        }

        override fun clone(): Percent {
            return Percent(amount).also { it.px = px }
        }

        override fun set(parent: Unit) {
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
                set(value) = run { field = value; vMin = Math.min(vWidth, vHeight); vMax = Math.max(vWidth, vHeight) }
            internal var vHeight = 0f
                set(value) = run { field = value; vMin = Math.min(vWidth, vHeight); vMax = Math.max(vWidth, vHeight) }
        }
    }


    /** represents the index of a flex component */
    class Flex(val index: Int = -1, val flexGrow: Int = 0, val flexBasis: Concrete? = null) : Unit(Units.Flex) {
        override var px: Float = 0f

        init {
            if (flexGrow < 0) {
                throw IllegalArgumentException("flexGrow cannot be negative.")
            }
        }

        override fun clone(): Flex {
            return Flex(index, flexGrow, flexBasis?.clone())
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




