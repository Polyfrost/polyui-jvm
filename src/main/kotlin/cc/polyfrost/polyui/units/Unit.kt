package cc.polyfrost.polyui.units

/**
 * class to represent a unit of measurement.
 */
abstract class Unit(var px: Float, val type: Type) : Cloneable {

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
        Pixel,
    }

    override fun toString(): String {
        return "Unit(px=$px, type=$type)"
    }

    // I don't know why Kotlin is wierd like this, and it still has cast warnings?
    public abstract override fun clone(): Unit

    class Pixel(pixels: Float) : Unit(pixels, Type.Pixel), Cloneable {
        override fun clone(): Pixel {
            return Pixel(px)
        }
    }

}




