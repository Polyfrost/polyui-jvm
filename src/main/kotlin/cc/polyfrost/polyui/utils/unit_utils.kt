@file:JvmName("UnitUtils")
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

    @JvmStatic
    fun auto() = FlexAuto

    @JvmField
            /** represents the automatic flex index. */
    val FlexAuto = Vec2(Unit.Flex(-1), Unit.Flex(-1))
}