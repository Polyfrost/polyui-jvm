package cc.polyfrost.polyui.utils

import cc.polyfrost.polyui.units.Unit
import cc.polyfrost.polyui.units.Vec2

operator fun Float.compareTo(x: Unit): Int {
    return this.compareTo(x.px)
}

fun Float.px(): Unit.Pixel {
    return Unit.Pixel(this)
}

fun Int.px(): Unit.Pixel {
    return Unit.Pixel(this.toFloat())
}

fun Double.px(): Unit.Pixel {
    return Unit.Pixel(this.toFloat())
}

fun Int.seconds(): Long {
    return this * 1000L
}

object UnitUtils {
    @JvmStatic
    fun index(index: Int): Unit.Flex {
        return Unit.Flex(index)
    }

    /** create a new flex unit, in vec2 form for at properties. */
    @JvmStatic
    fun flex(index: Int = -1, flexGrow: Int = 1, flexBasis: Unit.Concrete? = null): Vec2<Unit> {
        val u = Unit.Flex(index, flexGrow, flexBasis)
        return Vec2(u, u)
    }
}