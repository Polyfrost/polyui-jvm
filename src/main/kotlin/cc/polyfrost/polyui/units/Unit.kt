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

    class Pixel(pixels: Float) : Unit(Type.Pixel), Cloneable {
        override var px: Float = pixels
        override fun clone(): Pixel {
            return Pixel(px)
        }
    }

    /** represents the index of a flex component */
    class Flex(val index: Int) : Unit(Units.Flex) {
        init {
            if (index < 0) {
                throw Exception("Flex index cannot be less than 0")
            }
        }

        override var px by Delegates.notNull<Float>()
        override fun clone(): Flex {
            return Flex(index)
        }
    }

}

typealias Units = Unit.Type




