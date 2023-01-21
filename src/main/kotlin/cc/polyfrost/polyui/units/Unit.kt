package cc.polyfrost.polyui.units

/**
 * class to represent a unit of measurement. the pixels parameter is the value in pixels of the unit.
 * The update function is called
 */
abstract class Unit(var v: Float, vararg val dependantOn: Unit) {
    /** this function should recalculate the value of this.
     * It may call all its dependant units to ask them for their values.  */
    protected abstract fun update()

    var invalid = false

    operator fun plus(other: Unit): Float {
        return v + other.v
    }

    fun get(): Float {
        if(invalid) update()
        return v
    }

    operator fun compareTo(x: Unit): Int {
        return v.compareTo(x.v)
    }

    class Pixel(pixels: Float, vararg dependantOn: Unit) : Unit(pixels, *dependantOn) {
        override fun update() {
            TODO()
        }
    }
}




