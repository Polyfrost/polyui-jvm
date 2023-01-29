package cc.polyfrost.polyui.units

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
        Pixel, Flex
    }

    override fun toString(): String {
        return "Unit(px=$px, type=$type)"
    }

    public abstract override fun clone(): Unit

    class Pixel(pixels: Float) : Unit(Type.Pixel), Concrete {
        override var px: Float = pixels
        override fun clone(): Pixel {
            return Pixel(px)
        }
    }

    /** represents the index of a flex component */
    class Flex(val index: Int = -1, val flexGrow: Int = 0, val flexBasis: Concrete? = null) : Unit(Units.Flex) {
        override var px by Delegates.notNull<Float>()

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
    }
}

typealias Units = Unit.Type




