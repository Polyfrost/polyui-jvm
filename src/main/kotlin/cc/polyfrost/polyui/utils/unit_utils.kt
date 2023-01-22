package cc.polyfrost.polyui.utils

import cc.polyfrost.polyui.units.Unit

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