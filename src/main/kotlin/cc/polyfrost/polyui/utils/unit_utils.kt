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

/** unit that is a percent of its parent's size. */
fun Int.percent(): Unit.Percent {
    return Unit.Percent(this.toFloat())
}

/** unit that is a percent of its parent's size. */
fun Double.percent(): Unit.Percent {
    return Unit.Percent(this.toFloat())
}

/** unit that is a percent of the width of this viewport. */
fun Int.vwidth(): Unit.VUnits {
    return Unit.VUnits(this.toFloat(), Unit.Type.VWidth)
}

/** unit that is a percent of the height of this viewport. */
fun Int.vheight(): Unit.VUnits {
    return Unit.VUnits(this.toFloat(), Unit.Type.VHeight)
}

/** unit that is a percent of the smallest of the two dimensions (width, height) of this viewport.*/
fun Int.vmin(): Unit.VUnits {
    return Unit.VUnits(this.toFloat(), Unit.Type.VMin)
}

/** unit that is a percent of the largest of the two dimensions (width, height) of this viewport.*/
fun Int.vmax(): Unit.VUnits {
    return Unit.VUnits(this.toFloat(), Unit.Type.VMax)
}

/** unit that is a percent of the width of this viewport. */
fun Float.vwidth(): Unit.VUnits {
    return Unit.VUnits(this, Unit.Type.VWidth)
}

/** unit that is a percent of the height of this viewport. */
fun Float.vheight(): Unit.VUnits {
    return Unit.VUnits(this, Unit.Type.VHeight)
}

/** unit that is a percent of the smallest of the two dimensions (width, height) of this viewport.*/
fun Float.vmin(): Unit.VUnits {
    return Unit.VUnits(this, Unit.Type.VMin)
}

/** unit that is a percent of the largest of the two dimensions (width, height) of this viewport.*/
fun Float.vmax(): Unit.VUnits {
    return Unit.VUnits(this, Unit.Type.VMax)
}

/** amount in milliseconds */
fun Int.milliseconds(): Long {
    return this.toLong()
}

/** convert seconds to milliseconds */
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

    @JvmStatic
            /** fill as a vec */
    fun fillv(): Vec2<Unit> {
        return Vec2(fill(), fill())
    }


    @JvmStatic
            /** fill as a unit */
    fun fill(): Unit {
        return Unit.Percent(100f)
    }
}